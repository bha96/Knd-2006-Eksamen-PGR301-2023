package com.example.s3rekognition.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.Celebrity;
import com.amazonaws.services.rekognition.model.DetectProtectiveEquipmentRequest;
import com.amazonaws.services.rekognition.model.DetectProtectiveEquipmentResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.ProtectiveEquipmentSummarizationAttributes;
import com.amazonaws.services.rekognition.model.RecognizeCelebritiesRequest;
import com.amazonaws.services.rekognition.model.RecognizeCelebritiesResult;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.example.s3rekognition.FamousPerson;
import com.example.s3rekognition.MyImage;
import com.example.s3rekognition.PPEClassificationResponse;
import com.example.s3rekognition.PPEResponse;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@RestController
public class RekognitionController implements ApplicationListener<ApplicationReadyEvent> {

    private final AmazonS3 s3Client;
    private final AmazonRekognition rekognitionClient;
    private MeterRegistry meterRegistry;
    ListObjectsV2Result imageList;
    private AtomicInteger currentImageCount = new AtomicInteger(0);
    private AtomicInteger totalFamousPeopleCount = new AtomicInteger(0);
    private AtomicInteger imagesToTestForFamous = new AtomicInteger(0);

    private static final Logger logger = Logger.getLogger(RekognitionController.class.getName());

    @Autowired
    public RekognitionController(MeterRegistry meterRegistry) {
        this.s3Client = AmazonS3ClientBuilder.standard().build();
        this.rekognitionClient = AmazonRekognitionClientBuilder.standard().build();
        this.meterRegistry = meterRegistry;
    }

    /**
     * This endpoint takes an S3 bucket name in as an argument, scans all the
     * Files in the bucket for Protective Gear Violations.
     * <p>
     *
     * @param bucketName
     * @return
     */
    @Timed("ppe.timed")
    @GetMapping(value = "/scan-ppe", consumes = "*/*", produces = "application/json")
    @ResponseBody
    public ResponseEntity<PPEResponse> scanForPPE(@RequestParam String bucketName) {
        // List all objects in the S3 bucket
        imageList = s3Client.listObjectsV2(bucketName);
        

        // This will hold all of our classifications
        List<PPEClassificationResponse> classificationResponses = new ArrayList<>();

        // This is all the images in the bucket
        List<S3ObjectSummary> images = imageList.getObjectSummaries();

        // Iterate over each object and scan for PPE
        for (S3ObjectSummary image : images) {
            logger.info("scanning " + image.getKey());

            // This is where the magic happens, use AWS rekognition to detect PPE
            DetectProtectiveEquipmentRequest request = new DetectProtectiveEquipmentRequest()
                    .withImage(new Image()
                            .withS3Object(new S3Object()
                                    .withBucket(bucketName)
                                    .withName(image.getKey())))
                    .withSummarizationAttributes(new ProtectiveEquipmentSummarizationAttributes()
                            .withMinConfidence(80f)
                            .withRequiredEquipmentTypes("FACE_COVER"));

            DetectProtectiveEquipmentResult result = rekognitionClient.detectProtectiveEquipment(request);

            // If any person on an image lacks PPE on the face, it's a violation of regulations
            boolean violation = isViolation(result);

            logger.info("scanning " + image.getKey() + ", violation result " + violation);
            // Categorize the current image as a violation or not.
            int personCount = result.getPersons().size();
            PPEClassificationResponse classification = new PPEClassificationResponse(image.getKey(), personCount, violation);
            classificationResponses.add(classification);
        }
        PPEResponse ppeResponse = new PPEResponse(bucketName, classificationResponses);
        return ResponseEntity.ok(ppeResponse);
    }

    /**
     * Detects if the image has a protective gear violation for the FACE bodypart-
     * It does so by iterating over all persons in a picture, and then again over
     * each body part of the person. If the body part is a FACE and there is no
     * protective gear on it, a violation is recorded for the picture.
     *
     * @param result
     * @return
     */
    private static boolean isViolation(DetectProtectiveEquipmentResult result) {
        return result.getPersons().stream()
                .flatMap(p -> p.getBodyParts().stream())
                .anyMatch(bodyPart -> bodyPart.getName().equals("FACE")
                        && bodyPart.getEquipmentDetections().isEmpty());
    }
    @Timed("famous.timed")
    @GetMapping(value = "/famous-peeps", consumes = "*/*", produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<MyImage>> checkFamousPeople(@RequestParam String bucketName){
        
        //All images
        imageList = s3Client.listObjectsV2(bucketName);
        
        currentImageCount.set(imageList.getKeyCount());
        
        logger.info("images in bucket: " + currentImageCount);
        
        //Only images with name containing "famous"
        List<S3ObjectSummary> famousPeopleList = new ArrayList<>();
        
        int imagesWithFamousInKeyCount = 0;
        //Adding the famous images
        for (S3ObjectSummary image : imageList.getObjectSummaries()) {
            if(image.getKey().contains("famous")){
                famousPeopleList.add(image);
                imagesWithFamousInKeyCount++;
            }
        }
        
        imagesToTestForFamous.set(imagesWithFamousInKeyCount);
        
        
        List<MyImage> images = new ArrayList<>();
        
        int famousPeopleCount = 0;
        for (S3ObjectSummary image : famousPeopleList) {
            RecognizeCelebritiesRequest request = new RecognizeCelebritiesRequest()
                .withImage(new Image()
                    .withS3Object(new S3Object()
                    .withBucket(bucketName)
                    .withName(image.getKey())));    
                    
            RecognizeCelebritiesResult result = rekognitionClient.recognizeCelebrities(request);
            
            List<FamousPerson> famousPersons = new ArrayList<>();
            
            for (Celebrity celebrity : result.getCelebrityFaces()) {
                FamousPerson famousPerson = new FamousPerson(celebrity.getName(), celebrity.getUrls());
                famousPersons.add(famousPerson);
                
                //Incrementing famous.person + celebrity name
                meterRegistry.counter("famous.person", "celebrity.name", celebrity.getName()).increment();
            }

            MyImage famousPersonImage = new MyImage(image.getKey(), result.getCelebrityFaces().size(), famousPersons);
            
            images.add(famousPersonImage);
            famousPeopleCount += result.getCelebrityFaces().size();
        }
        
        totalFamousPeopleCount.set(famousPeopleCount);
        
        
        
        
        return ResponseEntity.ok(images);
        
        
        
    }
    
    

    @Override
        public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
            Gauge.builder("image_count", currentImageCount, AtomicInteger::get).register(meterRegistry);
            Gauge.builder("famousPeopleInBucket", totalFamousPeopleCount, AtomicInteger::get).register(meterRegistry);
            Gauge.builder("imagesWithFamousInKey", imagesToTestForFamous, AtomicInteger::get).register(meterRegistry);
    }
}
