
	
// width, height & padding of the graph area
var width = 600, height = 400, padding = 50;

var exchangeRatesXml = null;
var dates = new Array();
var rateNames = new Array();
var selectedRateNames = new Array();
var dates = new Array();
var haveCheckboxes = false;

function refreshExchangeRates(url) {
	console.log("Sending Request...");
	$.ajax({
	    url: url,
	    async: true,
	}).done(function (response) {
		console.log("Response Received...");
		var xmlDoc = $.parseXML(response);
		exchangeRatesXml = $(xmlDoc);
		
		// get list of all available exchange rates
		var currencies = exchangeRatesXml.find("Cube[currency!='']").each(function(){
			var currency = $(this).attr("currency");
			if(rateNames.indexOf(currency) == -1 && currency != undefined)
				rateNames.push(currency);
		});
		
		// create a checkbox for each rate
		createCheckboxes(rateNames);
		
		// graph the data
		compileExchangeRateData();
	});
	
	console.log("Awaiting Response...");
}

function createCheckboxes(rateNames) {
	if(haveCheckboxes) return;
	
	haveCheckboxes = true;
	var rates = $('ul#rates');
	$.each(rateNames, function(iteration, item) {
		rates.append(
			$(document.createElement("li"))
			.append(
				$(document.createElement("input")).attr({
					id:	'rate-'+item,
					name: item,
					value: item,
					type: 'checkbox',
					checked: false
				})
				.click( function( event ) {
					compileExchangeRateData();
				})
			)
			.append(
				$(document.createElement('label')).
					attr({'for':'rate'+'-'+item}).text( item )
			)
		)
	});
}

function compileExchangeRateData(){	
	var selectedRates = new Array();
	selectedRateNames = new Array();
	
	// get data for all checkboxes that are checked
	$('input[type=checkbox]').each(function () {
		if(this.checked && $(this).val() != "Average") {
			selectedRateNames.push($(this).val());
			selectedRates.push(getRateData($(this).val()));
		}
	});
	
	// if nothing checked then default to USD	
	if(selectedRates.length == 0) {
		// check USD checkbox
		$('#rate-USD').prop('checked', true);
		
		// get data for USD
		selectedRateNames.push("USD");
		selectedRates.push(getRateData("USD"));
	}
	
	// construct the graph
	drawGraph(selectedRates);
}

function getRateData(rateId) {
	var rate = new Array();
	var weeklyAverages = new Array();
	var rollingAverage = new Array();
	dates = new Array();
	
	var rates = exchangeRatesXml.find("Cube[currency='"+rateId+"']");
	rates.each(function( index ) {	
		// store the actual rate	
		rate.push(parseFloat($(this).attr("rate")));		
		
		// store the dates
		var date = new Date($(this).parent().attr("time"));
		dates.push($(this).parent().attr("time"));
		
		// calculate a rolling average
		var average = rate.reduce(function(pv, cv) { return pv + cv; }, 0) / rate.length;
		rollingAverage.push(average);
		
		// store the weekly averages
		if(weeklyAverages[date.getWeek()] == undefined) {		
			weeklyAverages[date.getWeek()] = parseFloat($(this).attr("rate"));
		} else {
			weeklyAverages[date.getWeek()] += parseFloat($(this).attr("rate"));
			weeklyAverages[date.getWeek()] /= 2;
		}
	});
	
	if($('#option1').is(":checked")) {
		return rollingAverage;
	} else {
		return rate;
	}
}

function drawGraph(selectedRates) {	
	var data = selectedRates;
	
	// remove existing graph
	d3.select("#chart").remove();

	// find min and max of data to be graphed
	// TODO better way?
	var newArr = [];
	for(var i=0; i < data.length; i++) {
		newArr = newArr.concat(data[i]);
		
		// also reverse the arrays to have earliest data first
		data[i].reverse();
	}
	/*var maxRate = data.reduce(function(max, arr) { 
					return Math.max(max, arr[0]); 					
				  }, -Infinity);
	var minRate = data.reduce(function(max, arr) { 
					return Math.min(max, arr[0]); 
				  }, Infinity);*/
	var minValue = d3.min(newArr);
	var maxValue = d3.max(newArr);
	
	// append the svg tag to the document
	var svg = d3.select("body")
		.append("svg")
		.attr("id", "chart")
		.attr("width", width)
		.attr("height", height);

	// create a group for the data
	var lines = svg.selectAll("g").data(data);
	
	// X scale
	var xLinear = d3.scale.linear()
		.domain([0, data[0].length])
		.range([padding, width-padding]);
	var minDate = new Date(dates[dates.length-1]),
		maxDate = new Date(dates[0]);
	var x = d3.time.scale().domain([minDate, maxDate]).range([padding, width-padding]);

	// Y scale
	var y = d3.scale.linear()
		.domain([minValue, maxValue])
		.range([height-padding, padding]);
		
	// create the line
	var line = d3.svg.line()
		//.interpolate("step-before")
		.x(function(d,i) { return xLinear(i); })
		.y(function(d) { return y(d); });
		
	// a start value for the transition
	var startvalue = d3.svg.line()
		.x(function(d) {return xLinear(0);})
		.y(function(d) {return y(d);})	
	
	// for each array of data, create a 'g' line container
	var aLineContainer = lines.enter().append("g").attr("class", "currency");  	
	aLineContainer.append("path")
		.attr("class", "line")
		.on("mouseover", function(d, i) {      
			div.transition()        
				.duration(200)      
				.style("opacity", .9);      
			div.html(selectedRateNames[i])  
				.style("left", (d3.event.pageX) + "px")     
				.style("top", (d3.event.pageY - 28) + "px");    
		})                  
		.on("mouseout", function(d) {       
			div.transition()        
				.duration(500)      
				.style("opacity", 0);
		})     
		.attr("d", startvalue(data[0]))
		.transition().duration(1000).delay(500)                
		.attr("d", line)
		.style("stroke", function(d) { return getRandomColor(); });
		
	// append div for tooltips
	var div = d3.select("body").append("div")   
		.attr("class", "tooltip")               
		.style("opacity", 0);
	
	// draw dots for each data point
	aLineContainer.selectAll(".dot")
		.data( function( d, i ) { return d; } )
		.enter()
		.append("circle")
		.on("mouseover", function(d) {      
			div.transition()        
				.duration(200)      
				.style("opacity", .9);      
			div.html(d)  
				.style("left", (d3.event.pageX) + "px")     
				.style("top", (d3.event.pageY - 28) + "px");    
		})                  
		.on("mouseout", function(d) {       
			div.transition()        
				.duration(500)      
				.style("opacity", 0);
		})
		.transition()
		.delay(1500)
		.attr("class", "dot")
		.attr("cx", line.x())
		.attr("cy", line.y())
		.attr("r", 3);
	
	// X-Axis
	var xAxis = d3.svg.axis().scale(x).orient("bottom");
	var xAxisGroup = svg.append("g")
		.attr("transform", "translate(0," + (height - padding) + ")")
		.attr("class", "axis")
		.call(xAxis)
		.selectAll("text")  
            .style("text-anchor", "end")
            .attr("dx", "-.8em")
            .attr("dy", ".15em")
            .attr("transform", function(d) {
                return "rotate(-65)" 
                });
		
	// Y-Axis
	var yAxis = d3.svg.axis().scale(y).orient("left");
	var yAxisGroup = svg.append("g")
		.attr("transform", "translate(" + padding + ", 0)")
		.attr("class", "axis")
		.call(yAxis);
}