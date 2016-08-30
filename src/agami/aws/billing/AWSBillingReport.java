package agami.aws.billing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.sun.corba.se.spi.oa.OADefault;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import au.com.bytecode.opencsv.CSVReader;

public class AWSBillingReport {
	private static Logger logger = Logger.getLogger(AWSBillingReport.class);
	private static String bucketName = "*****************";	
	static Date date = new Date();
	static ISO8601DateFormat iso = new ISO8601DateFormat();
	static String isoDate = iso.format(date);
	static String subDate = isoDate.substring(0, 7);
	private static String key = "112252525-aws-cost-allocation-" + subDate + ".csv";
	static Properties prop = new Properties();
	static {
		InputStream input;
		try {
			input = new FileInputStream("/home/********/path.properties");
			if (input != null) {
				try {
					prop.load(input);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws IOException {
		
		try {

			logger.info("Downloading an object from S3.............");
			ClientConfiguration config = new ClientConfiguration();
			System.out.println(config.getSocketTimeout());
			config.setSocketTimeout(180000);
			AWSCredentials credentials = new BasicAWSCredentials("*************",
					"************************************");
			AmazonS3 client = new AmazonS3Client();
			AmazonS3 s3Client = new AmazonS3Client(credentials, config);
			S3Object s3object = s3Client.getObject(new GetObjectRequest(bucketName, key));
			logger.info("Content-Type: " + s3object.getObjectMetadata().getContentType());
			convertCSVtoExcel(s3object.getObjectContent());
			displayTextInputStream();

		} catch (AmazonServiceException ase) {
			logger.error("Caught an AmazonServiceException, which" + " means your request made it "
					+ "to Amazon S3, but was rejected with an error response" + " for some reason.");
			logger.error("Error Message:    " + ase.getMessage());
			logger.error("HTTP Status Code: " + ase.getStatusCode());
			logger.error("AWS Error Code:   " + ase.getErrorCode());
			logger.error("Error Type:       " + ase.getErrorType());
			logger.error("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			logger.error("Caught an AmazonClientException, which means" + " the client encountered "
					+ "an internal error while trying to " + "communicate with S3, "
					+ "such as not being able to access the network.");
			logger.error("Error Message: " + ace.getMessage());
		}
	}

	private static void displayTextInputStream() throws IOException {
		LinkedHashSet<Object> set = new LinkedHashSet<Object>();
		File file = new File(prop.getProperty("ExcelFilePath"));
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

					}
					set.add(prop.getProperty("AmazonRDS"));
					set.add(prop.getProperty("AmazonS3"));
					set.add(prop.getProperty("AmazonEC2"));
					if (flag == 1 && set.size() > 0) {
						try {
							List<Object[]> list = new ArrayList<Object[]>();
							List<Object[]> list1 = null;
							Iterator<Object> itr = set.iterator();
							while (itr.hasNext()) {
								String tagName = (String) itr.next();
								if (tagName != "," && !tagName.isEmpty() && tagName != null) {
									list1 = searchSheet(tagName, sheet);
									list.addAll(list1);
								}

							}
							list.add(0,new Object[]{"Billing Month",subDate});
							list.add(1, new Object[] { "Instance Name", "Amount($)" });
							double totalValue = 0.0d;
							for (int j = 0; j < list.size(); j++) {
								Object[] obj = list.get(j);
								for (int p = 0; p < obj.length; p++) {
									if (obj[p] instanceof Double) {
										totalValue = totalValue + (Double) obj[p];

									} else if (obj[p] instanceof String) {
									}
								}
							}
							logger.info("total Amount : " + totalValue);
							list.add(list.size(), new Object[] { "Total Amount($)", totalValue });
							getFinalResult(list);
						} catch (Exception e) {
							logger.error(e);
						}
					}
					if (flag == 0) {
						System.out.println("Record not found");
					}
				}
			}
		} catch (Exception ex) {
			System.out.println(ex);
		} finally {
			fis.close();
			if (file.exists()) {
				file.delete();
			}

		}
	}

	private static void convertCSVtoExcel(InputStream input) throws IOException {
		logger.info("Staring convert CSV file into Excel File");
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
			logger.info("succuessfully Converted CSV file into Excel File");
		} catch (Exception ex) {
			logger.error(ex);
		}
	}

	public static List<Object[]> searchSheet(String searchText, HSSFSheet sheet) {
		logger.info("Start searching record on the basis of TagName ");
		List<Object[]> filteredRows = new ArrayList<Object[]>();
		double total = 0.0d;
		double totalEC2 = 0.0d;
		try {
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
				}
				if (searchText.trim().equals("CHB-CONN")) {
					searchText += " (Amazon Virtual Private Cloud)";
				}

				filteredRows.add(new Object[] { searchText.trim(), total });
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return filteredRows;
	}

	private static void getFinalResult(List<Object[]> list) throws IOException {
		logger.info("Starting get final result......");
		FileOutputStream os = null;
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

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			os.close();
			logger.info("File Closed..");
		}
		logger.info("Start sending email....................");
		EmailSender.emailSender(prop.getProperty("FileUrl"));
	}

}