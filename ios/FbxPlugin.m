//
//  FbxPlugin.m
//
//  Created by anasit on 2018/5/12.
//  Copyright © 2018年 anasit. All rights reserved.
//

#import "FbxPlugin.h"

@implementation FbxPlugin{
    CDVPluginResult* pluginResult;
}

- (void)call:(CDVInvokedUrlCommand *)command{
     pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"IOS无关闭方法"];
     [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end