# AWSMonthlyBillingReport

1- Installing Java 8 on your system

       $ sudo add-apt-repository ppa:webupd8team/java
       $ sudo apt-get update
       $ sudo apt-get install oracle-java8-installer
       $ sudo apt-get install oracle-java8-set-default
       
2-  Download project from Github

       * change in properties file where you want to store file
      i.e:
        ExcelFilePath=/home/folderName/workbook.xls
        FileUrl=/home/folderName/tempWorkbook.xls
        RootTagName=cell value
         
3- AWS Credentials 

      AWSCredentials credentials = new BasicAWSCredentials(String accessKey,String secretKey);
			AmazonS3 client = new AmazonS3Client();
			AmazonS3 s3Client = new AmazonS3Client(credentials, config);
			S3Object s3object = s3Client.getObject(new GetObjectRequest(bucketName, key));
			logger.info("Content-Type: " + s3object.getObjectMetadata().getContentType());
			convertCSVtoExcel(s3object.getObjectContent());// called this method for convert CSV file into Excel file
			displayTextInputStream(); //Called this method for get value from excelsheet and send an email with attachment.
			
4-  Working with the Convert CSV File into Excel File

    HSSFWorkbook wb = new HSSFWorkbook();
		try {
			HSSFSheet sheet = wb.createSheet("new sheet");
			CSVReader reader = new CSVReader(new InputStreamReader(input));
			String[] line;
			int r = 0;
			while ((line = reader.readNext()) != null) {
				HSSFRow row = sheet.createRow((short) r++);
				for (int i = 0; i < line.length; i++)
					row.createCell((short) i).setCellValue(line[i]);
			}
			// Write the output to a file
			FileOutputStream fileOut = new FileOutputStream(prop.getProperty("ExcelFilePath"));
			logger.info(fileOut);
			wb.write(fileOut);
			fileOut.close();
			
5-  Searching with given cell value

    if (searchText != null) {
				// Iterate rows
				for (int j = 0; j <= sheet.getLastRowNum(); j++) {
					HSSFRow row = sheet.getRow(j);
					// Iterate columns
					for (int k = 0; k < row.getLastCellNum(); k++) {
						HSSFCell cell = row.getCell((short) k);
						// Search value based on cell type
						switch (cell.getCellType()) {
						case HSSFCell.CELL_TYPE_STRING:
							if (searchText.trim() != null
									&& searchText.trim().equals(cell.getStringCellValue().trim())) {
								if (searchText.trim().equals("AmazonEC2")
										&& row.getCell((short) 30).getStringCellValue().equals("")) {
									total += Double.parseDouble(row.getCell((short) 29).getStringCellValue());
								} else if (!searchText.trim().equals("AmazonEC2")) {
									total += Double.parseDouble(row.getCell((short) 29).getStringCellValue());
								} else {

								}

							}
							break;
						}
					}
					
				
6-    Working with Worksheet / Cell Object Description
	
	        FileInputStream fis = new FileInputStream(file);
	      	int flag = 0;
	      	try {
		    	if (fis != null) {
			   	HSSFWorkbook workbook = new HSSFWorkbook(fis);
				  for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
					HSSFSheet sheet = workbook.getSheetAt(i);
					Iterator<Row> rowIterator = sheet.rowIterator();
					while (rowIterator.hasNext()) {
						HSSFRow row = (HSSFRow) rowIterator.next();
						Iterator<Cell> cellIterator = row.cellIterator();
						while (cellIterator.hasNext()) {
							HSSFCell cell = (HSSFCell) cellIterator.next();
							if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
								if (prop.getProperty("RootTagName").trim().equals(cell.getStringCellValue().trim())) {
									flag = 1;

									for (int p = 2; p <= sheet.getLastRowNum(); p++) {
										if (sheet.getRow(sheet.getFirstRowNum() + p)
												.getCell(cell.getCellNum()) != null) {
											cell = sheet.getRow(sheet.getFirstRowNum() + p).getCell(cell.getCellNum());
											set.add(cell.getStringCellValue());
										}

									}
								}
							}
						}
						
7-  Woking with Sheet Formate/Cell Formate and add row value into arrayList on the basis of cell value and write row value another Excel File


          HSSFWorkbook finalWorkSpace = new HSSFWorkbook();
	      	HSSFSheet newSheet = finalWorkSpace.createSheet("MonthlyBillingReport");
	      	//newSheet.setDefaultRowHeight((short) 0);
		      newSheet.setColumnWidth((short) 0, (short) 20000);
	      	HSSFFont font = finalWorkSpace.createFont();
		      font.setColor(HSSFFont.COLOR_RED);
	      	// font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		      HSSFCellStyle cellStyle = finalWorkSpace.createCellStyle();
          cellStyle.setFont(font);
	      	cellStyle.setFillForegroundColor(HSSFColor.LIGHT_YELLOW.index);
		      cellStyle.setBorderBottom((short) 1);
		      cellStyle.setFillPattern(cellStyle.SOLID_FOREGROUND);
		      cellStyle.setBorderBottom(cellStyle.BORDER_THIN);
	      	try {
		  	  HashMap<Object, Object[]> map = new HashMap<Object, Object[]>();
		  	  for (int k = 0; k < list.size(); k++) {
				  map.put(k, list.get(k));
		    	}
			    Set<Object> entry = map.keySet();
		      int rownum = 0;
			    for (Object key : entry) {
				  // Creating a new Row in existing XLSX sheet
			  	HSSFRow row = newSheet.createRow(rownum++);
			  	Object[] objArr = (Object[]) map.get(key);
			  	int cellnum = 0;
			  	for (Object obj : objArr) {
					HSSFCell cell = row.createCell((short) cellnum++);
					cell.setCellStyle(cellStyle);
					if (obj instanceof String) {
						cell.setCellValue((String) obj);
					} else if (obj instanceof Boolean) {
						cell.setCellValue((Boolean) obj);
					} else if (obj instanceof Date) {
						cell.setCellValue((Date) obj);
					} else if (obj instanceof Double) {
						cell.setCellValue(new DecimalFormat("##.##").format(obj));
					}
				  }
			    }
		    	// open an OutputStream to save written data into
			   os = new FileOutputStream(new File(prop.getProperty("FileUrl")));
			   finalWorkSpace.write(os);
			   logger.info("Writing on XLSX file Finished ...!:)");
			  ********************************************XXXXX***************************************
