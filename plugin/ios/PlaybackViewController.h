//
//  PlaybackViewController.h
//  督贝督导端
//
//  Created by  anasit on 2019/2/12.
//

#import <UIKit/UIKit.h>
#import <EZOpenSDKFramework/EZOpenSDK.h>
#import <EZOpenSDKFramework/EZPlayer.h>
#import <EZOpenSDKFramework/EZDeviceInfo.h>
#import <EZOpenSDKFramework/EZDeviceRecordFile.h>
#import <EZOpenSDKFramework/EZCloudRecordFile.h>
#import "EZPlaybackProgressBar.h"
#import "DatePickerController.h"
#import "EOAVolumeUtil.h"

@interface PlaybackViewController : UIViewController
- (void) setCamere :(NSString *)cameraName :(NSString *)cameraSeries :(NSInteger)cameraNo;
- (void)updatePickerDate:(NSString *)identifier :(NSDate *)date;
@end
