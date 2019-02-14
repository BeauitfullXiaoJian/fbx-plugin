module.exports = function (ctx) {
    var cordova_util = ctx.requireCordovaModule("cordova-lib/src/cordova/util"),
        ConfigParser = ctx.requireCordovaModule('cordova-common').ConfigParser,
        child_process = ctx.requireCordovaModule('child_process'),
        path = ctx.requireCordovaModule('path'),
        deferral = ctx.requireCordovaModule('q').defer();
    var platformRoot = path.join(ctx.opts.projectRoot, 'platforms/ios');
    var projectXml = cordova_util.projectConfig(ctx.opts.projectRoot);
    var projectConfig = new ConfigParser(projectXml);
    var projectName = projectConfig.name();
    var sourePath = __dirname + '/images';
    var targetPath = platformRoot + '/' + projectName + '/Images.xcassets';
    child_process.spawn('cp', ['-r', sourePath + '/Camera.imageset', targetPath]);
    child_process.spawn('cp', ['-r', sourePath + '/RecordActive.imageset', targetPath]);
    child_process.spawn('cp', ['-r', sourePath + '/Start.imageset', targetPath]);
    child_process.spawn('cp', ['-r', sourePath + '/PlaybackBackground.imageset', targetPath]);
    child_process.spawn('cp', ['-r', sourePath + '/SoundAdd.imageset', targetPath]);
    child_process.spawn('cp', ['-r', sourePath + '/Stop.imageset', targetPath]);
    child_process.spawn('cp', ['-r', sourePath + '/Record.imageset', targetPath]);
    child_process.spawn('cp', ['-r', sourePath + '/SoundLess.imageset', targetPath]);
    console.log("拷贝目录", projectName, sourePath, targetPath);
    deferral.resolve();
    return deferral.promise;
}