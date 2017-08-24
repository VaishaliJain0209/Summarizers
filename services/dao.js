var mysql = require('mysql');
var express = require('express');

var pool = mysql.createPool({
  connectionLimit : 10,
  host: "127.0.0.1",//"hack21.cabvnnxdzue9.ap-south-1.rds.amazonaws.com",
  user: "root",
  password: "root",
  port: '3306',
  database: 'hack2'
});

exports.getSecurities = function(next){
    console.log('trying connections');
    pool.getConnection(function(err, connection) {
        console.log("connected");
        var sql = "Select id,name from hack2.security";
        connection.query( sql, function(error, results, fields) {
              if (err) throw err;
              console.log("Result: " + results);
              connection.release();
              return next(results);
        });
    });
}