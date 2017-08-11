var mysql = require('mysql');
var express = require('express');

var con = mysql.createConnection({
  host: "hack21.cabvnnxdzue9.ap-south-1.rds.amazonaws.com",
  user: "firs",
  password: "sapient2017",
  port: '3306',
  database: 'hack2'
});

exports.getSecurities = function(next){
    console.log('trying connections');
    con.connect(function(err) {
      if (err) throw err;
      console.log("Connected!");
      var sql = "Select id,name from hack2.security";
      con.query(sql, function (err, result) {
          if (err) throw err;
          console.log("Result: " + result);
          return next(result);
      });
    });
}
