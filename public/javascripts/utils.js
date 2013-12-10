// Move to utils file
function getRandomColor() {    
	return "rgb("+Math.floor((Math.random()*255)+1)+","+
		Math.floor((Math.random()*255)+1)+","+
		Math.floor((Math.random()*255)+1)+")";
}

// add a get week method to the date class
Date.prototype.getWeek = function() {
	var onejan = new Date(this.getFullYear(),0,1);
	return Math.ceil((((this - onejan) / 86400000) + onejan.getDay()+1)/7);
}