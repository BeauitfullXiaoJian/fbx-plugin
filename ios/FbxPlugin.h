//
//  FbxPlugin.h
//
//  Created by anasit on 2018/5/12.
//  Copyright © 2018年 anasit. All rights reserved.
//

#import <Cordova/CDVPlugin.h>

@interface FbxPlugin : CDVPlugin
{
}

- (void)call:(CDVInvokedUrlCommand*)command;

@end