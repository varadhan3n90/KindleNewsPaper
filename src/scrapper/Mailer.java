package scrapper;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Mailer {
	public void sendMailWithAttachment(String from,String to,String subject,String body,File file) throws IOException {	
	    final String username = "shivajithebossss@gmail.com";
	    final String password = "shivajitheboss";	
	    Properties props = new Properties();
	    props.put("mail.smtp.auth", true);
	    props.put("mail.smtp.starttls.enable", true);
	    props.put("mail.smtp.host", "smtp.gmail.com");
	    props.put("mail.smtp.port", "587");
	
	    Session session = Session.getInstance(props,
	            new javax.mail.Authenticator() {
	                protected PasswordAuthentication getPasswordAuthentication() {
	                    return new PasswordAuthentication(username, password);
	                }
	            });
	
	    try {	
	        Message message = new MimeMessage(session);
	        message.setFrom(new InternetAddress(from));
	        message.setRecipients(Message.RecipientType.TO,
	                InternetAddress.parse(to));
	        message.setSubject(subject);
	        message.setText(body);	
	        MimeBodyPart messageBodyPart = new MimeBodyPart();	
	        Multipart multipart = new MimeMultipart();	
	        messageBodyPart = new MimeBodyPart();	        
	        messageBodyPart.attachFile(file);	        
	        multipart.addBodyPart(messageBodyPart);	
	        message.setContent(multipart);	
	        System.out.println("Sending");	
	        Transport.send(message);	
	        System.out.println("Done");	
	    } catch (MessagingException e) {
	        e.printStackTrace();
	    }
	}
	
}