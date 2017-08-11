
var columnDefs = [
    {headerName: "Effective Date", field: "effDate", width: 180},
    {headerName: "Price", field: "price", width: 180},
    {headerName: "OAS", field: "oas", width: 180},
    {headerName: "Yield", field: "yield", width: 180}
];

/*var rowData = [
        {
        		"security": 1234567,
                "effDate": "2017-06-29",
                "price": 29.84,
                "oas" : 10.12
        },
        {
                "effDate": "2017-06-28",
                "price": 33.34,
                "oas" : 12.12
        },
        {
                "effDate": "2017-06-27",
                "price": 31.62,
                "oas" : 11.13
        },
        {
                "effDate": "2017-06-26",
                "price": 30.37,
                "oas" : 11.13
        },
        {
                "effDate": "2017-06-25",
                "price": 31.51,
                "oas" : 18.11
        },
        {
                "effDate": "2017-06-24",
                "price": 33.37,
                "oas" : 17.19
        },
        {
                "effDate": "2017-06-23",
                "price": 35.03,
                "oas" : 13.21
        },
        {
                "effDate": "2017-06-22",
                "price": 34.94,
                "oas" : 17.10
        },
        {
                "effDate": "2017-06-21",
                "price": 36.34,
                "oas" : 21.09
        },
        {
                "effDate": "2017-06-20",
                "price": 22.84,
                "oas" : 19.98
        }
];*/

var gridOptions = {
    columnDefs: columnDefs,
    //animateRows: true,
    //enableRangeSelection: true,
    rowData: [],
    enableSorting:true
};

/*document.addEventListener('DOMContentLoaded', function() {
	setTimeout(function(){drawTable();},2000);

    });*/

/*// setup the grid after the page has finished loading
document.addEventListener('DOMContentLoaded', function() {
    var gridDiv = document.querySelector('#myGrid');
    new agGrid.Grid(gridDiv, gridOptions);

    // do http request to get our sample data - not using any framework to keep the example self contained.
    // you will probably use a framework like JQuery, Angular or something else to do your HTTP calls.
    var httpRequest = new XMLHttpRequest();
    httpRequest.open('GET', '../olympicWinners.json');
    httpRequest.send();
    httpRequest.onreadystatechange = function() {
        if (httpRequest.readyState == 4 && httpRequest.status == 200) {
            var httpResult = JSON.parse(httpRequest.responseText);
            gridOptions.api.setRowData(httpResult);
        }
    };
});*/