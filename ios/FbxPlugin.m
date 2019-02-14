//
//  FbxPlugin.m
//
//  Created by anasit on 2018/5/12.
//  Copyright © 2018年 anasit. All rights reserved.
//

#import "FbxPlugin.h"
#import "PlaybackViewController.h"

@interface FbxPlugin ()<EZPlayerDelegate>

@property CDVPluginResult* pluginResult;

@property (strong, nonatomic) UIView *mUIView;

@property EZPlayer *mActivePlayer;

@property EZPlayer *mActiveTalker;

@property EZDeviceInfo *mDeviceInfo;

@property NSInteger mPlayStatus;

@end

@implementation FbxPlugin

// CordovaPlugin 初始化方法

- (void)pluginInitialize
{
    self.mPlayStatus = 0;
}

// CordovaPlugin 对外开放调用方法

/**
 * 直播一个摄像头
 */
- (void)live:(CDVInvokedUrlCommand *)command{
    [self log:@"启动直播"];
    NSString* appKey = [command.arguments objectAtIndex:0];
    NSString* accessToken = [command.arguments objectAtIndex:1];
    NSString* cameraSerial = [command.arguments objectAtIndex:2];
    NSString* cameraNo = [command.arguments objectAtIndex:3];
    [self initSDK:appKey accessToken:accessToken];
    [self addPlayView];
    [self initPlayer:cameraSerial cameraNo:[cameraNo integerValue]];
    [self initTalker:cameraSerial cameraNo:[cameraNo integerValue]];
}

/**
 * 隐藏播放窗口
 */
- (void)hideLive:(CDVInvokedUrlCommand *)command{
    [self.mUIView setHidden:YES];
}

/**
 * 显示播放窗口
 */
- (void)showLive:(CDVInvokedUrlCommand *)command{
    [self.mUIView setHidden:NO];
}

/**
 * 对讲操作
 */
- (void)talk:(CDVInvokedUrlCommand *)command{
    NSString* action = [command.arguments objectAtIndex:0];
    // NSString* cameraSerial = [command.arguments objectAtIndex:1];
    if([action isEqualToString:@"prepare"]){
        // 开启对讲功能
        [self.mActiveTalker startVoiceTalk];
    }else if([action isEqualToString:@"close"]){
        // 关闭对讲功能
        [self.mActiveTalker startVoiceTalk];
    }else if([action isEqualToString:@"say"]){
        [self log:@"开始说话"];
        [self.mActiveTalker audioTalkPressed:YES];
    }else if([action isEqualToString:@"stop"]){
        [self log:@"停止说话"];
        [self.mActiveTalker audioTalkPressed:NO];
    }
}

/**
 * 云台控制
 */
- (void)actionCtrl:(CDVInvokedUrlCommand *)command{
    [self log:@"云台控制"];
    NSString* action = [command.arguments objectAtIndex:0];
    NSString* direction = [command.arguments objectAtIndex:1];
    NSString* cameraSerial = [command.arguments objectAtIndex:2];
    NSString* cameraNo = [command.arguments objectAtIndex:3];
    EZPTZAction ptzAction = [action isEqualToString:@"start"]?EZPTZActionStart:EZPTZActionStop;
    EZPTZCommand ptzCommand = EZPTZCommandUp;
    if([direction isEqualToString:@"t"]){
        ptzCommand = EZPTZCommandUp;
    }else if([direction isEqualToString:@"b"]){
        ptzCommand = EZPTZCommandDown;
    }else if([direction isEqualToString:@"l"]){
        ptzCommand = EZPTZCommandLeft;
    }else if([direction isEqualToString:@"r"]){
        ptzCommand = EZPTZCommandRight;
    }
    [EZOpenSDK controlPTZ:cameraSerial cameraNo:[cameraNo integerValue] command:ptzCommand action:ptzAction speed:0 result:nil];
}

/**
 * 抓图
 */
- (void)getPicture:(CDVInvokedUrlCommand *)command{
    [self log:@"进行抓图"];
    if(self.mPlayStatus == 1){
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
            UIImage* image = [self.mActivePlayer capturePicture:50];
            //            [self saveImageToPhotosAlbum:image];
            NSData* imageData = UIImagePNGRepresentation(image);
            NSString* imageBase64 = [imageData base64EncodedStringWithOptions:0];
            //             [self log:imageBase64];
            NSDictionary *datas = @{@"image": imageBase64};
            self.pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:datas];
            [self.commandDelegate sendPluginResult:self.pluginResult callbackId:command.callbackId];
        });
    }else{
        self.pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:self.pluginResult callbackId:command.callbackId];
    }
}

/**
 * 清理插件
 */
- (void)cleanPlugin:(CDVInvokedUrlCommand *)command{
    [self log:@"清理插件"];
    if(self.mUIView != nil) [self.mUIView setHidden:YES];
    if(self.mActivePlayer != nil) [self.mActivePlayer destoryPlayer];
    self.mActivePlayer = nil;
    
}

// 相关内部方法

/**
 * 初始化萤石云相关SDK
 *
 * @param appKey 应用钥匙
 * @param accessToken 授权令牌
 */
- (void)initSDK:(NSString*)appKey accessToken:(NSString*)accessToken {
    NSLog(@"初始化SDK");
    [EZOpenSDK setDebugLogEnable:NO];
    [EZOpenSDK initLibWithAppKey:appKey];
    [EZOpenSDK setAccessToken:accessToken];
}

/**
 * 创建播放视图
 */
- (void)addPlayView{
    if(self.mUIView == nil){
        CGRect screenRect = [ UIScreen mainScreen ].bounds;
        CGRect statusRect = [[UIApplication sharedApplication] statusBarFrame];
        self.mUIView = [[UIView alloc] initWithFrame:CGRectMake(0, statusRect.size.height, screenRect.size.width, screenRect.size.width/1.8)];
        [self.mUIView setBackgroundColor:UIColor.blackColor];
        [self.viewController.view addSubview:self.mUIView];
        UITapGestureRecognizer *doubleTapGesture = [[UITapGestureRecognizer alloc]initWithTarget:self action:@selector(fullOrSamllScreen:)];
        doubleTapGesture.numberOfTapsRequired =2;
        doubleTapGesture.numberOfTouchesRequired =1;
        [self.mUIView addGestureRecognizer:doubleTapGesture];
    }else{
        [self.mUIView setHidden:NO];
        CGRect rect; rect = [[UIApplication sharedApplication] statusBarFrame];
    }
}

-(void)fullOrSamllScreen:(UIGestureRecognizer *)sender{
    [self log:@"切换全屏/默认"];
    CGRect screenRect = [ UIScreen mainScreen ].bounds;
    CGRect statusRect = [[UIApplication sharedApplication] statusBarFrame];
    if(self.mUIView.bounds.size.width > screenRect.size.width){
        // 当前是全屏
        [UIView animateWithDuration:0.2 animations:^{
            self.mUIView.transform = CGAffineTransformRotate(self.mUIView.transform, 3*M_PI/2);
            CGRect newRect = CGRectMake(0, statusRect.size.height, screenRect.size.width, screenRect.size.width/1.8);
            self.mUIView.frame = newRect;
        }];
    }else{
        // 当前不是全屏
        [UIView animateWithDuration:0.2 animations:^{
            self.mUIView.transform = CGAffineTransformRotate(self.mUIView.transform, M_PI/2);
            CGRect newRect = CGRectMake(0, 0, screenRect.size.width, screenRect.size.height);
            self.mUIView.frame = newRect;
        }];
    }
}

/**
 * 初始化播放器
 */
- (void)initPlayer:(NSString*)cameraSerial  cameraNo:(NSInteger)cameraNo{
    if(self.mActivePlayer != nil){
        [self.mActivePlayer destoryPlayer];
    }
    self.mActivePlayer = [EZOpenSDK createPlayerWithDeviceSerial:cameraSerial cameraNo:cameraNo];
    self.mActivePlayer.delegate = self;
    [self.mActivePlayer setPlayerView:self.mUIView];
    [self.mActivePlayer startRealPlay];
}

/**
 * 初始化对讲
 */
-(void)initTalker:(NSString*)cameraSerial  cameraNo:(NSInteger)cameraNo{
    if(self.mActiveTalker != nil){
        [self.mActiveTalker destoryPlayer];
    }
    self.mActiveTalker = [EZOpenSDK createPlayerWithDeviceSerial:cameraSerial cameraNo:cameraNo];
    self.mActiveTalker.delegate = self;
}

/**
 * 获取设备信息
 * @param cameraSerial 设备序列号
 */
- (void)getCameraInfo:(NSString*)cameraSerial{
    __weak __typeof__(self) weakSelf = self;
    [EZOpenSDK getDeviceInfo:cameraSerial completion:^(EZDeviceInfo *deviceInfo, NSError *error) {
        [weakSelf log:@"获取设备信息"];
        weakSelf.mDeviceInfo = deviceInfo;
        [self.mActiveTalker startVoiceTalk];
    }];
}

/**
 * 打开直播故事面板
 */
- (void)openLiveStoryboard{
    UIStoryboard *liveStoryboard = [UIStoryboard storyboardWithName:@"LiveStoryboard" bundle:nil];
    UIViewController *liveViewController = [liveStoryboard instantiateViewControllerWithIdentifier:@"LiveViewController"];
    [self.viewController presentViewController:liveViewController animated:YES completion:nil];
}

- (void)saveImageToPhotosAlbum:(UIImage *)savedImage
{
    PHAuthorizationStatus status = [PHPhotoLibrary authorizationStatus];
    if (status == PHAuthorizationStatusNotDetermined)
    {
        [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
            if(status == PHAuthorizationStatusAuthorized)
            {
                UIImageWriteToSavedPhotosAlbum(savedImage, self, @selector(imageSavedToPhotosAlbum:didFinishSavingWithError:contextInfo:), NULL);
            }
        }];
    }
    else
    {
        if (status == PHAuthorizationStatusAuthorized)
        {
            UIImageWriteToSavedPhotosAlbum(savedImage, self, @selector(imageSavedToPhotosAlbum:didFinishSavingWithError:contextInfo:), NULL);
        }
    }
}

- (void)imageSavedToPhotosAlbum:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo:(void *)contextInfo
{
    NSString *message = nil;
    if (!error) {
        message = NSLocalizedString(@"device_save_gallery", @"已保存至手机相册");
    }
    else
    {
        message = [error description];
    }
}

/**
 * 日志打印
 * @param 日志信息
 */
- (void)log:(NSString*)message{
    NSLog(@"FbxPluginLog%@", message);
}

#pragma mark - delegate

/**
 *  播放器消息回调
 *
 *  @param player      播放器对象
 *  @param messageCode 播放器消息码，请对照EZOpenSDK头文件中的EZMessageCode使用
 */
- (void)player:(EZPlayer *)player didReceivedMessage:(NSInteger)messageCode{
    NSLog(@"接收到消息");
    switch (messageCode)
    {
        case PLAYER_REALPLAY_START:
        {
            NSLog(@"视频直播开始");
            // 开启直播声音
            [player openSound];
            self.mPlayStatus = 1;
            break;
        }
        case PLAYER_VOICE_TALK_START:
        {
            NSLog(@"语音对讲开始");
            [self.mActivePlayer closeSound];
        }
        case PLAYER_VOICE_TALK_END:
        {
            NSLog(@"语音对讲结束");
            [self.mActivePlayer openSound];
        }
    }
}

/**
 *  播放器播放失败错误回调
 *
 *  @param player 播放器对象
 *  @param error  播放器错误
 */
- (void)player:(EZPlayer *)player didPlayFailed:(NSError *)error{
    NSLog(@"视频直播失败:%@", error);
    // 重新开启直播
    [player startRealPlay];
}

- (void)playback:(CDVInvokedUrlCommand *)command{
    [self log:@"启动回放"];
    NSString* appKey = [command.arguments objectAtIndex:0];
    NSString* accessToken = [command.arguments objectAtIndex:1];
    NSString* cameraName = [command.arguments objectAtIndex:2];
    NSString* cameraSerial = [command.arguments objectAtIndex:3];
    NSString* cameraNo = [command.arguments objectAtIndex:4];
    [self initSDK:appKey accessToken:accessToken];
    UIStoryboard *playbackStoryboard = [UIStoryboard storyboardWithName:@"PlaybackStoryboard" bundle:nil];
    PlaybackViewController *playbackViewController = [playbackStoryboard instantiateViewControllerWithIdentifier:@"PlaybackViewController"];
    [playbackViewController setCamere :cameraName :cameraSerial :[cameraNo integerValue]];
    [self.viewController presentViewController:playbackViewController animated:YES completion:nil];
}

@end
