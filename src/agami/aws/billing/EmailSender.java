package agami.aws.billing;
import java.io.File;
import java.util.*;  
import javax.mail.*;  
import javax.mail.internet.*;

import org.apache.log4j.Logger;

import javax.activation.*;
public class EmailSender {
	static Logger logger = Logger.getLogger(EmailSender.class);
		public static void emailSender(String fileUrl) {
				String to="deepak.kumar@agamitechnologies.com";//change accordingly babul@1stdatasolutions.com 
			  final String userName="ms.deepak456@gmail.com";//change accordingly  
			  final String password="8651632077";//change accordingly 
			  String host = "smtp.gmail.com";
		      String port = "587";
			   
			  //1) get the session object     
			  Properties properties = System.getProperties();  
			  	properties.put("mail.smtp.host", host);
		        properties.put("mail.smtp.port", port);
		        properties.put("mail.smtp.auth", "true");
		        properties.put("mail.smtp.starttls.enable", "true");
		        properties.put("mail.user", userName);
		        properties.put("mail.password", password);
			  
		     // creates a new session with an authenticator
		        Authenticator auth = new Authenticator() {
		            public PasswordAuthentication getPasswordAuthentication() {
		                return new PasswordAuthentication(userName, password);
		            }
		        };
		        Session session = Session.getInstance(properties, auth);
			  //2) compose message     
			  try{  
				  logger.info("Start sending email with attachemnt");
			    MimeMessage message = new MimeMessage(session);  
			    message.setFrom(new InternetAddress(userName)); 
			    InternetAddress[] address = InternetAddress.parse(to);
			    message.setRecipients(Message.RecipientType.TO, address);  
			    message.setSubject("Monthly Billing Report"); 
			    message.setSentDate(new Date());
			      
			    //3) create MimeBodyPart object and set your message text     
			    BodyPart messageBodyPart1 = new MimeBodyPart();  
			   // messageBodyPart1.setText("This is message body"); 
			    messageBodyPart1.setContent("Monthly billing report of all AWS Instances has been attached. Please check  attachment !", "text/html;charset=utf-8");
			     
			    //4) create new MimeBodyPart object and set DataHandler object to this object      
			    MimeBodyPart messageBodyPart2 = new MimeBodyPart();  
			    if(fileUrl !=null){
			    DataSource source = new FileDataSource(fileUrl);  
			    messageBodyPart2.setDataHandler(new DataHandler(source));  
			    messageBodyPart2.setFileName(fileUrl);  
			    }
			    
			    //5) create Multipart object and add MimeBodyPart objects to this object      
			    Multipart multipart = new MimeMultipart();  
			    multipart.addBodyPart(messageBodyPart1);  
			    multipart.addBodyPart(messageBodyPart2);  
			  
			    //6) set the multipart object to the message object  
			    message.setContent(multipart );  
			     
			    //7) send message  
			    Transport.send(message);
			    File file = new File(fileUrl);
			    if(file.exists()){
			    	file.delete();
			    }
			   
			    logger.info("Email message sent....successfully! :) ");  
			   }catch (MessagingException ex) {ex.printStackTrace();}  
			 }  
	}


