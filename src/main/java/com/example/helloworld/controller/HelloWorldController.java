package com.example.helloworld.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import dto.Attemptsrequest;
import dto.GetDateUtil;
import dto.PaymentRequest;
import dto.TransactionPayload;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class HelloWorldController {

    @GetMapping("/CheckTooManyAttempts")
    public String CheckTooManyAttempts(@RequestParam String requestId,@RequestParam String paymentType)
    {
        try {
            System.out.println("start");
           // System.out.println(attemptsrequest);
            String host = "redis://default:ApeBBPaOxbyw8C9HCFxFucFw6laxBaHl@redis-14790.c100.us-east-1-4.ec2.cloud.redislabs.com:14790";
            int port = 0;
            int intervalTimeInSeconds = 10;
            String redisKey = "TransactionPayload";
            //String requestId = "1";

            return CheckTooManyAttempt(requestId, paymentType, host,
                    redisKey,
                    intervalTimeInSeconds
            );

        } catch (Exception e) {
            System.out.println(e.toString());

        }
        return "OK";
    }



    public static String CheckIntervalTime(List<TransactionPayload> existRequests, Date currentDateTime,
                                          String requestId,
                                         int intervalTimeInSeconds

    ) {
        try {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            String updatedJsonString = gson.toJson(existRequests);
            ObjectMapper mapper = new ObjectMapper();
            TransactionPayload[] checkintervallist = mapper.readValue(updatedJsonString, TransactionPayload[].class);

            Date expireddate = null;
            try {
                expireddate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(checkintervallist[0].getExpiredAt());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            //region Log
            //log.info("request Id => " + requestId + " CheckIntervalCount Fun => " + " currentDateTime => " + currentDateTime);
            //log.info("request Id => " + requestId + " CheckIntervalCount Fun => " + " expireddate => " + expireddate);
            //endregion

            if (currentDateTime.compareTo(expireddate) > 0) {
                //region Log
                //log.info("request Id => " + requestId + " CheckIntervalCount Fun => " + " currentDateTime occurs after expireddate ");
                //endregion
            } else {
                //region Log
                //log.info("request Id => " + requestId + " CheckIntervalCount Fun => " + " currentDateTime occurs before expireddate");
                //log.info("request Id =>" + requestId + " Duplicated transaction call : the interval time between each transaction is too close with config value : " + intervalTimeInSeconds + " with result size : " + existRequests.size() + " for request id : " + requestId);
                //endregion

                //JwtUtil jwtUtil = new JwtUtil();
                //response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                //jwtUtil.setErrorResponse(response, "YB10001", "Too many attempts, please try again later", "Too many attempts, please try again later");
                System.out.println(currentDateTime + " Too Many Attempts");
                return currentDateTime + " Too Many Attempts";
            }
        } catch (Exception e) {
            //region LogError
            //log.error("request Id = > " + requestId + " CheckIntervalCount Fun => " + e.toString());
            //endregion
        }
        return "OK";
    }

    public static String CreateData(String Key, String PaymentType, int intervalTimeInSeconds, Jedis jedis, String redisKey) {

        try {

            //log.info("request Id = > " + Key + " CreateData Fun => " + "start");
            //System.out.println("request Id = > " + Key + " CreateData Fun => " + "start");
            List<Map<String, Object>> dataList = null;

            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            String currentDateTime = dateFormat.format(date);

            //log.info("request Id => " + Key + " CreateData Fun => " + "current Date time => " + currentDateTime);

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.SECOND, intervalTimeInSeconds);

            String expiredAtDate = dateFormat.format(cal.getTime());

            TransactionPayload transactionPayload = new TransactionPayload();
            transactionPayload.setKey(Key);
            transactionPayload.setPaymentType(PaymentType);
            transactionPayload.setExpiredAt(expiredAtDate);
            String newRecordJson = serializeToJson(transactionPayload);



           // System.out.println("checking redisKey");
            // Get the existing JSON list from Redis
            String existingJsonList = jedis.get(redisKey);

           // System.out.println("existingJsonList " + existingJsonList);

            if (existingJsonList != null) {
                //System.out.println("Insert");
                dataList = new Gson().fromJson(existingJsonList, List.class);
            } else {
               // System.out.println("Initialized");
                dataList = new ArrayList<>();

            }
           // System.out.println("newRecordJson " + newRecordJson);

            Map<String, Object> newRecordMap = new Gson().fromJson(newRecordJson, Map.class);

            //System.out.println("newRecordMap " + newRecordMap);
            // Add the new record to the data list
            dataList.add(newRecordMap);

            //System.out.println(dataList.size());

            // Convert the updated data list to JSON
            String updatedJsonList = new Gson().toJson(dataList);

            //System.out.println(updatedJsonList);

            // Store the updated JSON list back to Redis
            jedis.set(redisKey, updatedJsonList);

            //log.info("request Id => " + Key + " CreateData Fun => " + " Create Data in Redis");

            jedis.close();

            return expiredAtDate;


        } catch (Exception e) {
            //log.error("request Id => " + Key + " => " + e.toString());
            //System.out.println("request Id => " + Key + " => " + e.toString());
            jedis.close();

        } finally {
            //System.out.println("request Id => finally");
            jedis.close();
        }

        return "";
    }

    public static List<TransactionPayload> GetDateByRequestIDAndPaymentType(String _key, String _paymentType, Jedis jedis, String redisKey) {
        try {
           // log.info("request Id = > " + _key + " CheckIntervalTime Fun => " + "start");
            //System.out.println("request Id = > " + _key + " GetDateByRequestIDAndPaymentType => " + "start");

            Gson gson = new Gson();
            ObjectMapper mapper = new ObjectMapper();

            String jsonString = jedis.get(redisKey);
            //System.out.println("jsonString " + jsonString);
            List<TransactionPayload> existRequests = null;
            if (jsonString != null)
            {
                TransactionPayload[] transactionPayloadlist = mapper.readValue(jsonString, TransactionPayload[].class);
                String searchKey = _key;
                String searchpaymentType = _paymentType;
                existRequests = Arrays.stream(transactionPayloadlist).filter(
                        record -> searchKey.equals(record.getKey()) &&
                                searchpaymentType.equals(record.getPaymentType())
                ).collect(Collectors.toList());

                // log.info("request Id = > " + _key + " CheckIntervalTime Fun => " + existRequests);
            }
          

            jedis.close();
            return existRequests;
        } catch (Exception e) {
           // log.error("request Id => " + _key + " CheckIntervalTime Fun => " + e.toString());
            jedis.close();
        } finally {
            jedis.close();
        }
        return null;
    }

    public static void RemoveExpireData(String requestId, Jedis jedis, String redisKey) {
        try {

            //log.info("request Id = > " + requestId + " RemoveExpireData Fun => " + "start");
            //System.out.println("request Id = > " + requestId + " RemoveExpireData Fun => " + "start");
            Gson gson = new Gson();
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = jedis.get(redisKey);
            //System.out.println("jsonString "+jsonString);

            if (jsonString != null)
            {
                TransactionPayload[] transactionPayloadlist = mapper.readValue(jsonString, TransactionPayload[].class);
                List<TransactionPayload> TransactionPayloadupdate = new ArrayList<>();
                for (TransactionPayload transactionPayload : transactionPayloadlist) {
                    Date expireddate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(transactionPayload.getExpiredAt());
                    Date currentDateTime = GetDateUtil.getCurrentDateTime();
                    if (currentDateTime.compareTo(expireddate) > 0) {
                        //System.out.println("request Id = > " + requestId + " RemoveExpireData Fun => " + "currentDateTime occurs after expireddate");
                        //log.info("request Id = > " + requestId + " RemoveExpireData Fun => " + "currentDateTime occurs after expireddate");

                    } else {
                        //System.out.println("request Id = > " + requestId + " RemoveExpireData Fun => " + "currentDateTime occurs before expireddate");
                        //log.info("request Id = > " + requestId + " RemoveExpireData Fun => " + "currentDateTime occurs before expireddate");
                        TransactionPayloadupdate.add(transactionPayload);
                    }

                }

                String updatedJsonString = gson.toJson(TransactionPayloadupdate);
                Type listType = new TypeToken<ArrayList<JsonObject>>() {
                }.getType();

                List<JsonObject> jsonList = gson.fromJson(jsonString, listType);

                // Store the updated JSON string back to Redis
                jedis.set(redisKey, updatedJsonString);

                //log.info("request Id = > " + requestId + " RemoveExpireData Fun => " + "Remove Expired Data");
               //System.out.println("request Id = > " + requestId + " RemoveExpireData Fun => " + "Remove Expired Data");
            }


            //System.out.println("json is null");

            jedis.close();
        } catch (Exception e) {
           // System.out.println("request Id = > " + requestId + " RemoveExpireData Fun => " + e.toString());
            //log.error("request Id = > " + requestId + " RemoveExpireData Fun => " + e.toString());
            jedis.close();
        } finally {
            jedis.close();
        }


    }

    public static String CheckTooManyAttempt(String requestId,String paymentType, String host,
                                           String redisKey,
                                           int intervalTimeInSeconds
                                           ) {
        String expireDate = "";

        //System.out.println("CheckTooManyAttempt start");
        //System.out.println(requestId);
        if (requestId != null && !requestId.isEmpty()) {
            // Replace with your payload Object
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

           // PaymentRequest paymentRequest = gson.fromJson(((CustomHttpServletRequestWrapper) requestWrapper).getBody(), PaymentRequest.class);
            //region Log
           // log.info("To trace >>-- : -- requestId : " + requestId + " at path : " + path + " with payload : " + paymentRequest + " at timeInMilliSeconds : " + System.currentTimeMillis());
           // System.out.println("To trace >>-- : -- requestId : " + requestId + " with payload : " + "Other Account Transfer" + " at timeInMilliSeconds : " + System.currentTimeMillis());
            //endregion
            Date currentDateTime = GetDateUtil.getCurrentDateTime();

                Jedis jedis = new Jedis(host);

                try {
                    List<Map<String, Object>> dataList = null;
                    // Remove all request expired
                    RemoveExpireData(requestId, jedis, redisKey);

                    //Get Data by RequestID & Payment Type
                    List<TransactionPayload> existRequests = GetDateByRequestIDAndPaymentType(requestId, paymentType, jedis, redisKey);
                    //System.out.println("existsRequests "+ existRequests);


                    if (existRequests != null && existRequests.size() > 0) {
                            //Check Interval Time
                           return CheckIntervalTime(existRequests, currentDateTime,
                                    requestId,
                                    intervalTimeInSeconds);

                        } else {
                            //Create Data by request ID and Payment Type
                           // System.out.println("CreateData");
                           expireDate =  CreateData(requestId, paymentType, intervalTimeInSeconds, jedis, redisKey);
                        }



                    jedis.close();
                } catch (Exception e) {
                    //System.out.println(e.toString());
                    jedis.close();
                } finally {
                    jedis.close();
                }


        }
        System.out.println(expireDate + " OK");
        return expireDate + " OK";
    }

    private static String serializeToJson(TransactionPayload transactionPayload) {
        // Serialize the list to JSON (You can use any JSON serialization library of your choice)
        // Here, we'll use Gson for serialization to JSON.
//        Gson gson = new GsonBuilder()
//                .setLenient()
//                .create();
        Gson gson = new Gson();
        return gson.toJson(transactionPayload);
    }


}
