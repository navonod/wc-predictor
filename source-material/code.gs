POINTS = {
  "match": 1,
  "round": 2,
  "dow": 3,
  "date": 4,
  "time": 5,
  "team1": 7,
  "team1Score": 8,
  "team2Score": 9,
  "team2": 10,
  "team1Penalties": 11,
  "team2Penalties": 12
}

PREDICTIONS = {
  "prediction": 1,
  "result": 3
}

PREDICTIONS_RESPONSE = {
  "timestamp": 0,
  "name": 1,
  "pin": 2,
  "goldenBoot": 3,
  "goldenBall": 4,
  "goldenGlove": 5,
  "youngPlayer": 6,
  "groupA": 7,
  "groupB": 8,
  "groupC": 9,
  "groupD": 10,
  "groupE": 11,
  "groupF": 12,
  "groupG": 13,
  "groupH": 14,
  "fairPlay": 15,
  "entertaining": 16,
  "finalist1": 17,
  "finalist2": 18,
  "champion": 19
}

SPREADSHEET_WCP = "1GxeeH7WPqZUTu8RLen12sLD2c4V6z5jbLgDqAp-DWUA";

var predictionsSheet = SpreadsheetApp.getActive().getSheetByName("Predictions");
var pointsSheet = SpreadsheetApp.getActive().getSheetByName("Points");
var groupASheet = SpreadsheetApp.getActive().getSheetByName("Group A");
var groupBSheet = SpreadsheetApp.getActive().getSheetByName("Group B");
var groupCSheet = SpreadsheetApp.getActive().getSheetByName("Group C");
var groupDSheet = SpreadsheetApp.getActive().getSheetByName("Group D");
var groupESheet = SpreadsheetApp.getActive().getSheetByName("Group E");
var groupFSheet = SpreadsheetApp.getActive().getSheetByName("Group F");
var groupGSheet = SpreadsheetApp.getActive().getSheetByName("Group G");
var groupHSheet = SpreadsheetApp.getActive().getSheetByName("Group H");
var pinsSheet = SpreadsheetApp.getActive().getSheetByName("Pins");

function testHtml() {
  var template = getScoringTemplate();
  Logger.log(template.getCode());

}

function getCurrentRound() {
  var round = pointsSheet.getRange("A73:A73").getValues()[0][0];
  return round;
}

function testGetCurrentRound() {
  console.info(getCurrentRound());
}

/**
 * Get the URL for the Google Apps Script running as a WebApp.
 */
function getScriptUrl() {
  var url = ScriptApp.getService().getUrl();
  return url;
}

/** 
 * This is the main method called by any incoming GET request
 */
function doGet(e) {
  var page = e.parameters['page'];
  console.log(JSON.stringify(e));
  console.log("doGet page: "+page);
  var template = getScoringTemplate(page);
  var output = template.evaluate();
  output.setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
  return output;
}

function getScoringTemplate(file) {
  if (!file) file = 'index';
  console.info("Serving up page: "+file);
  var template = HtmlService.createTemplateFromFile(file);
  // console.info("Current Round: "+getCurrentRound());
  template.data = getMatchesForRound(getCurrentRound()+1);
  return template;
}

function getMatchesForRound(round) {
  return pointsSheet.getRange('A3:J72').getValues().filter(row => row[POINTS.round-1] == round)
}

function onOpen(event) {
  updateMenu();
}

function updateMenu() {
  var ui = SpreadsheetApp.getUi();
  ui.createMenu('WCP2022')
      .addItem("Round Predictions", "createRound")
      .addToUi();
}

function createRound(roundNumber) {
  var createRound = Browser.inputBox("Enter round number");
  // let url = serveRound(roundNumber);
  // let html = "<p style=\"font-family:sans-serif; font-size:15px\"><a target=\"_blank\" href=\""+url+"\">Click here to go the the questions form</a></p>.";
  // SpreadsheetApp.getUi().showModalDialog(HtmlService.createHtmlOutput(html), 'Round '+currentRound+' Open');
}

/** 
 * Win (correct score): 3 points
 * Win (but diff score): 1 point
 * Win (with same margin): 2 points
 * Draw (correct score): 3 points
 * Draw (but diff score): 1,5 point
 */
function scoreByMatchAndName(match, name) {

  var nameColumn = getPointsColumnForName(name);
  let matchRange = pointsSheet.getRange(match+2,1, match+2, POINTS.team2Penalties+nameColumn+3);

  let score1 = matchRange.getCell(1,POINTS.team1Score).getValue();
  let score2 = matchRange.getCell(1,POINTS.team2Score).getValue();
  let predict1 = matchRange.getCell(1,nameColumn+POINTS.team2Penalties-1).getValue();
  let predict2 = matchRange.getCell(1,nameColumn+POINTS.team2Penalties).getValue();

  return score(score1, score2, predict1, predict2);
}

function score(score1, score2, predict1, predict2) {
  if (isBlank(score1) || isBlank(score2) || isBlank(predict1) || isBlank(predict2))
    return "";

  let scoreDiff = score1-score2;
  let predictDiff = predict1-predict2;

  // Win (correct score): 3 points
  // Draw (correct score): 3 points
  if (score1 == predict1 && score2 == predict2)
    return 3;
 
  // Win (with same margin): 2 points
  if (scoreDiff != 0 && scoreDiff == predictDiff)
    return 2;

  // Draw (but diff score): 1,5 point
  if (score1 == score2 && predict1 == predict2)
    return 1.5;

  // Win (but diff score): 1 point
  if ((scoreDiff > 0 && predictDiff > 0) || (scoreDiff < 0 && predictDiff < 0))
    return 1;

  return 0;
}

function isBlank(value) {
  return value.toString() == "";
}

function getPointsRowForMatchId(matchId) {
  let matchRange = pointsSheet.getRange(1, 1, pointsSheet.getLastRow(), pointsSheet.getLastColumn());
  for (var row = 3; row <= pointsSheet.getLastRow(); row++) {
    let cell = matchRange.getCell(row, 1).getValue();
    if (cell == matchId)
      return row;
  }
  return null;
}

function getPointsColumnForName(searchName) {
  let lastColumn = pointsSheet.getLastColumn()-POINTS.team2Penalties+1;
  var names = pointsSheet.getRange(1,POINTS.team2Penalties+1,1,lastColumn);
  for (var count=1; count <= lastColumn; count += 4) {
    let cell = names.getCell(1,count);
    let name = cell.getValue();
    if(name == searchName)
      return count;
  }
  return 0;
}

function getPredictionsColumnForName(searchName) {
  let lastColumn = predictionsSheet.getLastColumn()-PREDICTIONS.result+1;
  var names = predictionsSheet.getRange(1,PREDICTIONS.result+1,1,lastColumn);
  // console.info(JSON.stringify(names));
  for (var count=1; count <= lastColumn; count += 3) {
    let cell = names.getCell(1,count);
    let name = cell.getValue();
    if(name == searchName)
      return count;
  }
  return 0;
}

function sourceForm(event) {
  let namedValues = event.namedValues;
  if (typeof namedValues["Group A"] != 'undefined') {
    return "predictions";
  }
  if (typeof namedValues[""] != 'undefined') {
    return "";
  }
  return undefined;
}

function onFormSubmitted(event) {
  console.info(JSON.stringify(event));
  let values = event.values;
  let name = values[PREDICTIONS_RESPONSE.name];
  let pin = values[PREDICTIONS_RESPONSE.pin];
  if (verify(name, pin)) {
    if (sourceForm(event) === "predictions")
      writePredictionsSheet(values, name);
  }
  return;
}

function writePredictionsSheet(values, name) {
      let predictionsSheet = SpreadsheetApp.openById(SPREADSHEET_WCP).getSheetByName("Predictions");
      let nameColumn = getPredictionsColumnForName(name);
      var writeRange = predictionsSheet.getRange(3, PREDICTIONS.result+nameColumn, values.length-1-3, 1);
      var writeRows = [];
      for (var count=3; count < values.length-1; count++)
          writeRows[count-3] = [values[count]];
      console.info(JSON.stringify(writeRows));
      writeRange.setValues(writeRows);
}

function verify(name, pin) {
  let pinsSheet = SpreadsheetApp.openById(SPREADSHEET_WCP).getSheetByName("Pins");
  let rows = pinsSheet.getLastRow();
  for (var count=1; count <= rows; count++) {
    var theName = pinsSheet.getRange(1,1,rows,2).getCell(count, 1).getValue();
    if (theName == name) {
      var thePin = pinsSheet.getRange(1,1,rows,2).getCell(count, 2).getValue();
      if (pin == thePin)
        return true;
      return false;
    }
  }
  return false;
}

function processPredictScoresForm(formObject) {

    let playerName = formObject.name;
    let pin = formObject.pin;
    if (!verify(playerName, pin))
      return;
    
    console.info(JSON.stringify(formObject));
    let roundNumber = formObject.roundNumber;
    let matches = getMatchesForRound(roundNumber);
    let predictColumn = getPointsColumnForName(playerName)+POINTS.team2Penalties;
    let firstMatchRow = getPointsRowForMatchId(matches[0][POINTS.match-1]);
    let lastMatchRow = getPointsRowForMatchId(matches[matches.length-1][POINTS.match-1]);
    let predictionRange = pointsSheet.getRange(firstMatchRow, predictColumn, lastMatchRow-firstMatchRow+1, 2);

    var result = [];
    for (var count = 0; count < matches.length; count++) {
      var match = matches[count];
      let matchId = match[POINTS.match-1];
      let team1PredictName = "match-"+matchId+"-team-1"
      let team2PredictName = "match-"+matchId+"-team-2"
      result.push([formObject[team1PredictName], formObject[team2PredictName]]);
    }
    predictionRange.setValues(result);
}

function testProcessPredictScoresForm() {
  let matchId = 1;
  /*var formObject = {"match-4-team-2":"6","match-12-team-1":"","match-9-team-2":"","match-8-team-2":"","match-5-team-2":"","pin":"3343","match-6-team-1":"","match-11-team-2":"","match-2-team-1":"3","match-3-team-2":"3","match-7-team-1":"","match-16-team-2":"","match-13-team-2":"","match-16-team-1":"","match-9-team-1":"","match-13-team-1":"","match-1-team-1":"4","match-12-team-2":"","match-3-team-1":"3","match-14-team-2":"","match-2-team-2":"8","match-8-team-1":"","match-4-team-1":"5","match-15-team-2":"","match-6-team-2":"","match-10-team-1":"","match-11-team-1":"","name":"Donovan","match-10-team-2":"","match-15-team-1":"","match-1-team-2":"7","match-14-team-1":"","match-5-team-1":"","match-7-team-2":"", "roundNumber":1};*/
  var formObject = {"match-57-team-1":"3","match-58-team-2":"3","roundNumber":"5","match-59-team-1":"3","pin":"5555","match-58-team-1":"3","match-60-team-2":"3","match-60-team-1":"3","name":"Ago","match-59-team-2":"3","match-57-team-2":"3"}
  processPredictScoresForm(formObject);
}

testData = `{
	"authMode": "FULL",
	"namedValues": {
		"Which team will be the second finalist?\\n5 points for any team that reaches the final.": [
			"England"
		],
		"Email address": [
			""
		],
		"Group D": [
			"France, Denmark"
		],
		"Group A": [
			"Senegal, Netherlands"
		],
		"Timestamp": [
			"11/11/2022 21:05:03"
		],
		"Group H": [
			"Portugal, Uruguay"
		],
		"Group C": [
			"Argentina, Mexico"
		],
		"Group E": [
			"Spain, Germany"
		],
		"Who'll win the Best Young Player award?\\nThe Best Young Player award is awarded to a player who is under 21 at the start of the calendar year.": [
			"Bellingam"
		],
		"Who'll win the Golden Boot?\\nThe Golden Boot is awarded to the player with the most goals during the tournament. Should there be a shared boot at the end of the tournament, you'll score 5 points for any of the players sharing the boot.": [
			"Mbop"
		],
		"Who'll win the Golden Glove?\\nThe Golden Glove is awarded to the player voted as the best goalkeeper of the tournament.": [
			"Alisson"
		],
		"Which country will be the 2022 FIFA World Cup Champions?": [
			"Argentina"
		],
		"Which team will win the Most Entertaining Team award?\\nAs voted for by a public opinion poll run by FIFA. ": [
			"Brazil"
		],
		"Who are you?": [
			"Sonia"
		],
		"Group G": [
			"Brazil, Cameroon"
		],
		"Which team will be the first finalist?\\n5 points for any team that reaches the final.": [
			"Germany"
		],
		"Group F": [
			"Belgium, Croatia"
		],
		"Who'll win the Golden Ball?\\nThe Golden Ball is awarded to the player voted as the best player of the tournament.": [
			"Maradona"
		],
		"Which team will win the FIFA Fair Play Trophy?\\nThe FIFA Fair Play Trophy is given to the team with the best record of fair play. ": [
			"Croatia"
		],
		"What's your pass code?": [
			"3321"
		],
		"Group B": [
			"England, USA"
		]
	},
	"range": {
		"columnEnd": 21,
		"columnStart": 1,
		"rowEnd": 7,
		"rowStart": 7
	},
	"source": {},
	"triggerUid": "13514908",
	"values": [
		"11/11/2022 21:05:03",
		"Selwyn",
		"5589",
		"Mbop",
		"Maradona",
		"Alisson",
		"Bellingam",
		"Senegal, Netherlands",
		"England, USA",
		"Argentina, Mexico",
		"France, Denmark",
		"Spain, Germany",
		"Belgium, Croatia",
		"Brazil, Cameroon",
		"Portugal, Uruguay",
		"Croatia",
		"Brazil",
		"Germany",
		"England",
		"Argentina",
		""
	]
}`;

function testOnFormSubmitted() {
  onFormSubmitted(JSON.parse(testData));
}

