//
//  FbxPlugin.h
//
//  Created by anasit on 2018/5/12.
//  Copyright © 2018年 anasit. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <Cordova/CDVPlugin.h>
#import <Photos/Photos.h>
#import <EZOpenSDKFramework/EZOpenSDK.h>
#import <EZOpenSDKFramework/EZPlayer.h>
#import <EZOpenSDKFramework/EZDeviceInfo.h>

@interface FbxPlugin : CDVPlugin
{
}

- (void)live:(CDVInvokedUrlCommand*)command;
- (void)hideLive:(CDVInvokedUrlCommand *)command;
- (void)showLive:(CDVInvokedUrlCommand *)command;
- (void)cleanPlugin:(CDVInvokedUrlCommand*)command;
- (void)talk:(CDVInvokedUrlCommand *)command;
- (void)actionCtrl:(CDVInvokedUrlCommand *)command;
- (void)getPicture:(CDVInvokedUrlCommand *)command;
- (void)playback:(CDVInvokedUrlCommand *)command;
@end
