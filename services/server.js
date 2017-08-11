// call the packages we need
var express    = require('express');        // call express
var app        = express();                 // define our app using express
var bodyParser = require('body-parser');
var s3Obj = require('./S3Object.js');
var dao = require('./dao.js');

// configure app to use bodyParser()
// this will let us get the data from a POST
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());


var port = process.env.PORT || 8080;        // set our port

// ROUTES FOR OUR API
// =============================================================================
var router = express.Router();              // get an instance of the express Router

// test route to make sure everything is working (accessed at GET http://localhost:8080/api)
router.get('/', function(req, res) {
    res.json({ message: 'hooray! welcome to our api!' });   
});

router.get('/getView', function(req, res){
    console.log(req.query);
    s3Obj.getData("/summarizersecurities/1234567/1yr", 'data.json', function(data){
        console.log('data returned');
        res.json(data);
    });
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
});

router.get('/getSecurities', function(req, res){
   res.header("Access-Control-Allow-Origin", "*");    
   res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
   /*dao.getSecurities(function(data){
       console.log(data.size);
       res.json(data);
   });*/
   res.json([{id:1234567, name:1234567}, {id:98765, name:98765}]);
});

router.get('/getSecurityAttrs', function(req, res){
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
   res.json([{val:'price' , text:'price'}, {val:'oas', text:'oas'} , {val:'yield', text:'yield'}]);
});


// more routes for our API will happen here

// REGISTER OUR ROUTES -------------------------------
// all of our routes will be prefixed with /api
app.use('/api', router);

// START THE SERVER
// =============================================================================
app.listen(port);
console.log('Magic happens on port ' + port);