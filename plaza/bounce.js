/*
 * Copyright (c) Mike Brickman 2014-2017
 */

var bouncy = require('bouncy');
var fs = require('fs');
var tls = require('tls');
var net = require('net');
var http = require('http');
var debug = require('debug')('bounce:server');


var port = 80;
var sslPort = 443;
var redirectPort = 8080;
var wellKnownPort = 5000; // used to generate certificates using certbot
var ip = 'localhost';
var configFile = "/data/kotlin/config.json";

process.argv.forEach(function (val, index, array) {
    var parts = val.split("=");
    if (parts[0] == "--port") {
        port = parts[1]
    }
    if (parts[0] == "--ssl-port") {
        sslPort = parts[1]
    }
    if (parts[0] == "--redirect-port") {
        redirectPort = parts[1]
    }
    if (parts[0] == "--ip") {
        ip = parts[1]
    }
    if (parts[0] == "--config") {
        configFile = parts[1]
    }
});

console.log("ip=" + ip + ", port=" + port + ", sslPort=" + sslPort + ", redirectPort=" + redirectPort + ", configFile=" + configFile);


var config = require(configFile);

var redirectServer = http.createServer(function (req, res) {
    var redirect = 'https://' + req.headers.host + req.url;
    console.log("Redirect to: " + redirect);
    res.writeHead(302, {'Location': redirect});
    res.end();
}).listen(redirectPort);

function redirect(secure, host, url, res) {
    var redirect = (secure ? 'https://' : 'http://') + host + url;
    console.log("Redirect to: " + redirect);
    res.writeHead(302, {'Location': redirect});
    res.end();
}

function do_bounce(req, res, bounce, isSsl) {
    for (index in config.sites) {
        var hosts = config.sites[index].hosts.split(",");
        var ip = config.sites[index].ip;
        var port = config.sites[index].port;
        var secure = config.sites[index].secure;
        for (hostIndex in hosts) {
            var host = hosts[hostIndex];
            if (req.headers.host === host) {
                if (!isSsl && secure) {
                    redirect(true, hosts[0], req.url, res);
                } else if (hostIndex>0) {
                    redirect(isSsl, hosts[0], req.url, res);
                } else if (ip == undefined) {
                    bounce(port, {
                        headers: {
                            'x-forwarded-for': req.socket.remoteAddress
                        }
                    });
                } else {
                    bounce(ip, port, {
                        headers: {
                            'x-forwarded-for': req.socket.remoteAddress
                        }
                    });
                }
                return;
            }
        }
    }
    res.statusCode = 404;
    res.end('no such host');
}

var ssl;
for (index in config.sites) {
    var cert = config.sites[index].cert;
    if (cert) {
        var key = config.sites[index].key;
        ssl = {
            key: fs.readFileSync(key),
            cert: fs.readFileSync(cert),
            SNICallback: sni_select
        };
        break;
    }
}

bouncy(function (req, res, bounce) {
    console.log('bounce (' + ip + ")" + req.headers.host + req.url);
    if (req.url.indexOf("/.well-known")!== -1) {
        bounce(ip, wellKnownPort)
    } else {
        do_bounce(req, res, bounce, false)
    }
}).listen(port);

if (ssl) {
    bouncy(ssl, function (req, res, bounce) {
        var ip = req.headers['x-forwarded-for'] || req.connection.remoteAddress;
        console.log('bounce_ssl ' + req.headers.host + req.url + ' (' + ip + ')');
        do_bounce(req, res, bounce, true)
    }).listen(sslPort);
}

function sni_select(hostname, cb) {
    console.log('sni_select ' + hostname);
    for (index in config.sites) {
        var hosts = config.sites[index].hosts.split(",");
        for (hostIndex in hosts) {
            var host = hosts[hostIndex];
            if (hostname === host) {
                var cert = fs.readFileSync(config.sites[index].cert);
                var key = fs.readFileSync(config.sites[index].key);
                var creds = {key: key, cert: cert};
                cb(null, tls.createSecureContext(creds).context);
                return;
            }
        }
    }
}

// ================================================
// Special server to process requests from certbot
// ================================================
var express = require("express");
var app = express();

/* serves all the static files */
app.get(/^(.+)$/, function(req, res){
    var folder="/data/node/www/bounce/" + req.headers.host + "/";
    console.log('static file request : ' + req.params);
    res.sendFile(folder + req.params[0]);
}).listen(wellKnownPort);



