function exportAllSheetsAsCSV() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheets = ss.getSheets();
  const folder = DriveApp.getFolderById(ss.getId()).getParents().next(); // Saves in the same folder as the spreadsheet
  
  sheets.forEach(sheet => {
    const csvData = convertSheetToCSV(sheet);
    const fileName = sheet.getName() + ".csv";
    
    // Delete existing file with same name (optional - prevents duplicates)
    const existingFiles = folder.getFilesByName(fileName);
    while (existingFiles.hasNext()) {
      existingFiles.next().setTrashed(true);
    }
    
    folder.createFile(fileName, csvData, MimeType.CSV);
  });
  
  SpreadsheetApp.getUi().alert('✅ All sheets exported as CSV to your Drive folder!');
}

function convertSheetToCSV(sheet) {
  const data = sheet.getDataRange().getDisplayValues();
  return data.map(row => 
    row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(',')
  ).join('\n');
}