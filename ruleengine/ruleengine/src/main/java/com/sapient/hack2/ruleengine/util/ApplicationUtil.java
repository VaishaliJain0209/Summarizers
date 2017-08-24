package com.sapient.hack2.ruleengine.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.gson.Gson;

/**
 * Utilities
 * @author ssh150
 *
 */
@Component
public class ApplicationUtil {
	
	private static final String DEFAULT_BUCKET = "firs-hack2";
	
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public AmazonS3 getS3Client () {
		AWSCredentials credentials = new BasicAWSCredentials(
				"AKIAIM7NBES73EJ6E7MA",
				"Rrcvw84Fx83yze5QMb5Ek+hp2dns5slcgTMGCoNg");
		return new AmazonS3Client(credentials);
	}
	
	public void s3FileRead () {
		for (Bucket bucket : getS3Client().listBuckets()) {
			System.out.println(" - " + bucket.getName());
		}
	}
	
	/**
	 * 
	 * @param key
	 * @param data
	 * @param metadata
	 */
	public void s3ObjectWrite(String key, InputStream data, ObjectMetadata metadata) {
		PutObjectRequest request = new PutObjectRequest(DEFAULT_BUCKET, key, data, metadata);
		getS3Client().putObject(request);
	}
	
	/**
	 * 
	 * @param key
	 * @param type
	 * @return
	 */
	public Object s3ObjectRead(String key, Type type) {
		
		Object obj = null;
		
		GetObjectRequest request = new GetObjectRequest(DEFAULT_BUCKET, key);
		S3Object object = getS3Client().getObject(request);
		
		if (object != null) {
			
			try (InputStreamReader is = new InputStreamReader(object.getObjectContent())){
				
				Gson gson = new Gson();
				obj = gson.fromJson(is, type);
				
			}catch (Exception exception) {
				exception.printStackTrace();
			}
			
		}
		
		return obj;
	}
	
	
	
}
