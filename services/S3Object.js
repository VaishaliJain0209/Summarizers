var config=require("./config.js");
var AWS = require('aws-sdk');
var uuid = require('node-uuid');
var proxy = require('proxy-agent');
var fs = require('fs');
var express = require('express');

exports.getData = function(bucketName, fileName, next){
    console.log('bucket name - '+ bucketName);
    console.log('filename - '+ fileName);
    updateCredentials(config.credentials.accessKeyId,config.credentials.secretAccessKey,config.credentials.proxy);
    var s3 = new AWS.S3({apiVersion: '2006-03-01'});
    s3.getObject({Bucket: bucketName, Key: fileName},function(err, json_data) {
          if (!err) {
              console.log("getting "+fileName+" data from S3 bucket");
              //console.log(new Buffer(json_data.Body).toString("utf8"));
              next(new Buffer(json_data.Body).toString("utf8"));
          }else{
              console.log(err);
          }
    });
}

function updateCredentials(accessKeyId,secretAccessKey,proxyName){
 AWS.config.update({region: 'ap-south-1', credentials: {
  accessKeyId : accessKeyId,
  secretAccessKey :secretAccessKey
 },
 httpOptions: "http://localhost"//{ agent: proxy(proxyName)}
});
}