function myFunction() {
  var spreadsheet = SpreadsheetApp.getActive();
  spreadsheet.getRangeList(['D:D', 'G:G', 'J:J', 'M:M', 'P:P', 'S:S', 'V:V']).activate();
  spreadsheet.getActiveSheet().hideColumns(spreadsheet.getActiveRange().getColumn(), spreadsheet.getActiveRange().getNumColumns());
  spreadsheet.getRange('E:E').activate();
};