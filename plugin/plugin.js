var exec = require('cordova/exec');

var FBX = {
    call: function (funcName, success, error, args) {
        exec(success, error, 'FbxPlugin', funcName, args);
    },
};

module.exports = FBX;
