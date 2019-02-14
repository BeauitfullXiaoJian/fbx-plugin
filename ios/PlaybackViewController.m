//
//  PlaybackViewController.m
//  督贝督导端
//
//  Created by  anasit on 2019/2/12.
//

#import "PlaybackViewController.h"

@interface PlaybackViewController ()<EZPlaybackProgressDelegate,EZPlayerDelegate>

@property NSString *cameraName;
@property NSString * cameraSeries;
@property NSInteger cameraNo;
@property NSDate * startTime;
@property NSDate * endTime;
@property EZPlaybackProgressBar * timeBar;
@property EZDeviceRecordFile *activeFile;
@property Boolean canChangeSound;
@property Boolean isFullScreen;
@property NSDate *activeFileTime;
@property NSMutableArray<EZDeviceRecordFile*> *videoList;
@property EZPlayer *player;
@property NSInteger playStatus;
@property Boolean isRecord;
@property NSString *recordPath;
@property CGRect viewRect;
@property (weak, nonatomic) IBOutlet UIView *playView;
@property (weak, nonatomic) IBOutlet UIButton *startBtn;
@property (weak, nonatomic) IBOutlet UIButton *endBtn;
@property (weak, nonatomic) IBOutlet UIButton *playBtn;
@property (weak, nonatomic) IBOutlet UILabel *cameraTitleView;
@property (weak, nonatomic) IBOutlet UIView *timeBarView;
@property (weak, nonatomic) IBOutlet UISlider *soundSlider;
@property (weak, nonatomic) IBOutlet UIActivityIndicatorView *loadingBar;
@property (weak, nonatomic) IBOutlet UIStackView *controlView;

@end

@implementation PlaybackViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    _playStatus = 0;
    _videoList = [NSMutableArray array];
    [_cameraTitleView setText:_cameraName];
    _soundSlider.value = [[EOAVolumeUtil shareInstance]  getSysVolume];
    NSLog(@"系统音量%f",[[EOAVolumeUtil shareInstance]  getSysVolume]);
    [self hideLoading];
    
    // 计算最佳尺寸
    UITapGestureRecognizer *doubleTapGesture = [[UITapGestureRecognizer alloc]initWithTarget:self action:@selector(fullOrSamllScreen:)];
    doubleTapGesture.numberOfTapsRequired =2;
    doubleTapGesture.numberOfTouchesRequired =1;
    [_playView addGestureRecognizer:doubleTapGesture];
}

- (void) viewWillDisappear:(BOOL)animated
{
    [super viewWillDisappear:animated];
    // [self stopLocalRecord];//如果正在录像需停止录像
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations
{
    return UIInterfaceOrientationMaskPortrait;
}

// 显示日期选择对话框
- (IBAction)showPickerDateDialog:(UIButton *)sender {
    NSLog(@"%@", sender.restorationIdentifier);
    DatePickerController *datePickerController = [self.storyboard instantiateViewControllerWithIdentifier:@"DatePickerController"];
    datePickerController.parentCtrl = self;
    datePickerController.identify = sender.restorationIdentifier;
    [self presentViewController:datePickerController animated:YES completion:nil];
}

// 修改系统音量
- (IBAction)changeSystemSound:(id)sender {
    if(_canChangeSound == YES){
        UISlider *slider = (UISlider *) sender;
        [[EOAVolumeUtil shareInstance]  setSysVolume:slider.value];
    }
    _canChangeSound = YES;
}


// 暂停/开始播放录像
- (IBAction)playOrStopPlayer:(id)sender {
        NSLog(@"按下播放按钮");
        if(_playStatus == 1){
            [self stop];
            [self setStopStatus];
        }else if(_playStatus == 2){
            [self play];
            [self setPlayStatus];
        }
}

// 保存快照
- (IBAction)savePicture:(id)sender {
    if(_player){
         UIImage *image = [_player capturePicture:100];
        UIImageWriteToSavedPhotosAlbum(image, self, @selector(imageSavedToPhotosAlbum:didFinishSavingWithError:contextInfo:), NULL);
    }
}

// 开始/结束录像
- (IBAction)startOrStopRecord:(id)sender {
    if(!_player){
        return;
    }
    UIButton *button = sender;
    if(_isRecord != YES){
        _recordPath = [self getLocalRecordFilePath];
        if (_recordPath){
            NSLog(@"保存地址%@",_recordPath);
            _isRecord = [_player startLocalRecordWithPath:_recordPath];
            if(_isRecord){
                [button setImage:[UIImage imageNamed:@"RecordActive"] forState:UIControlStateNormal];
            }
            NSLog(@"录制结果%hhu",_isRecord);
        }
    }else{
        Boolean ret = [_player stopLocalRecord];
        _isRecord = NO;
        NSLog(@"录制暂停结果%hhu",ret);
        if(ret){
            UISaveVideoAtPathToSavedPhotosAlbum(_recordPath, self, @selector(imageSavedToPhotosAlbum:didFinishSavingWithError:contextInfo:), NULL);
            NSLog(@"保存录像到相册%@",_recordPath);
             [button setImage:[UIImage imageNamed:@"Record"] forState:UIControlStateNormal];
        }
    }
}


- (void)imageSavedToPhotosAlbum:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo:(void *)contextInfo
{
    if (!error)
    {
        NSLog(@"保存成功");
    }
    else
    {
        NSLog(@"保存失败@");
    }
}


- (void)updatePickerDate:(NSString *)identifier :(NSDate *)date{
    NSDateFormatter *dateformatter=[[NSDateFormatter alloc] init];
    [dateformatter setDateFormat:@"yyyy/MM/dd"];
    NSString* dateString = [dateformatter stringFromDate:date];
    NSLog(@"选择的日期是：%@",dateString);
    if([identifier isEqualToString:@"start_date"]){
        _startBtn.titleLabel.text = dateString;
        _startTime = date;
    }
    if([identifier isEqualToString:@"end_date"]){
        _endBtn.titleLabel.text = dateString;
        _endTime = date;
    }
    if(_startTime!=nil&&_endTime!=nil){
        [self searchVideo:_startTime :_endTime];
    }
}

- (void) searchVideo :(NSDate *)startTime :(NSDate *)endTime{
    
    // 开启加载动画
    [self showLoading];
    
    [EZOpenSDK searchRecordFileFromDevice:_cameraSeries cameraNo:_cameraNo beginTime:startTime endTime:endTime completion:^(NSArray *deviceRecords, NSError *error) {
        
        // 查询结束关闭动画
        [self hideLoading];
        
        if(error == nil){
            NSLog(@"解析视频录像");
            [self updateTimeBar:deviceRecords];
        }else{
            NSLog(@"获取存储记录失败%@",error.localizedFailureReason);
        }
    }];
}

- (void) updateTimeBar:(NSArray *) list
{
    NSMutableArray *destList = [NSMutableArray array];
    for (id fileInfo in list)
    {
        EZPlaybackInfo *info = [[EZPlaybackInfo alloc] init];
        
        if  ([fileInfo isKindOfClass:[EZDeviceRecordFile class]])
        {
            info.beginTime = ((EZDeviceRecordFile*)fileInfo).startTime;
            info.endTime = ((EZDeviceRecordFile*)fileInfo).stopTime;
            info.recType = 2;
            [_videoList addObject:fileInfo];
        }
        else
        {
            info.beginTime = ((EZCloudRecordFile*)fileInfo).startTime;
            info.endTime = ((EZCloudRecordFile*)fileInfo).stopTime;
            info.recType = 1;
        }
        
        [destList addObject:info];
    }
    
    if (_timeBar == nil)
    {
        _timeBar = [[EZPlaybackProgressBar alloc] initWithFrame:CGRectMake(0, 0,[UIScreen mainScreen].bounds.size.width,80)dataList:destList];
         _timeBar.delegate = self;
        _timeBar.backgroundColor = [UIColor clearColor];
        [_timeBarView addSubview :_timeBar];
        [_timeBar scrollToDate:((EZPlaybackInfo*)[destList firstObject]).beginTime];
    }
    else{
        [_timeBar updateWithDataList:destList];
        [_timeBar scrollToDate:((EZPlaybackInfo*)[destList firstObject]).beginTime];
    }
}

- (void) preparePlayer :(EZDeviceRecordFile *)playFile :(NSDate *)playTime{
    
    // 显示加载动画
    [self showLoading];
    
    // 准备播放器
    if(_player==nil){
        NSLog(@"初始化播放器");
        _player = [EZOpenSDK createPlayerWithDeviceSerial:_cameraSeries cameraNo:_cameraNo];
        _player.delegate = self;
        [_player setPlayerView:_playView];
    }
    if(_activeFile == nil || playFile != _activeFile){
        NSLog(@"载入回放文件");
        _activeFile = playFile;
        [_player startPlaybackFromDevice:_activeFile];
        [_player openSound];
    }else{
        NSLog(@"跳转进度播放");
    }
    
    // 跳转到指定时间开始播放
    [_player seekPlayback:playTime];
     _activeFileTime = playTime;
}

- (void) EZPlaybackProgressBarScrollToTime:(NSDate *)time
{
    EZDeviceRecordFile* file = nil;
    // 找到这个时间所属片段
    for (EZDeviceRecordFile* video in _videoList){
        if([time compare:video.startTime]>=0&&[time compare:video.stopTime]<=0){
            file = video;
            break;
        }
    }
    if(file!=nil){
        NSLog(@"找到对应录像文件");
        [self preparePlayer:file :time];
    }else{
        NSLog(@"当前时间没有录像");
    }
}

- (void) setCamere :(NSString *)cameraName :(NSString *)cameraSeries :(NSInteger)cameraNo{
    _cameraName = cameraName;
    _cameraSeries = cameraSeries;
    _cameraNo = cameraNo;
}

- (void) setPlayStatus{
    NSLog(@"设置播放状态");
    _playStatus = 1;
    [_playBtn setImage:[UIImage imageNamed:@"Stop"] forState:UIControlStateNormal];
}

- (void) setStopStatus{
    NSLog(@"设置暂停状态");
    _playStatus = 2;
    [_playBtn setImage:[UIImage imageNamed:@"Start"] forState:UIControlStateNormal];
}

- (void) play{
    if(_player){
        [_player resumePlayback];
    }
}

- (void) stop{
    if(_player){
        [_player pausePlayback];
    }
}

- (void) showLoading{
    _loadingBar.hidden = false;
}

- (void) hideLoading{
    _loadingBar.hidden = true;
}

-(void)fullOrSamllScreen:(UIGestureRecognizer *)sender{
    NSLog(@"切换全屏/默认");
    CGRect screenRect = [ UIScreen mainScreen ].bounds;
    CGRect statusRect = [[UIApplication sharedApplication] statusBarFrame];
    
    if(_isFullScreen == YES){
        // 当前是全屏
        [UIView animateWithDuration:0.2 animations:^{
            _playView.transform = CGAffineTransformRotate(_playView.transform, M_PI/2);
            _playView.frame = _viewRect;
            _controlView.hidden = NO;
            _isFullScreen = NO;
        }];
    }else{
        // 当前不是全屏

        // 保存之前的尺寸
        _viewRect = _playView.frame;
        [UIView animateWithDuration:0.2 animations:^{
            _playView.transform = CGAffineTransformRotate(_playView.transform, M_PI/2);
            CGRect newRect = CGRectMake(0, 0, screenRect.size.width, screenRect.size.height- statusRect.size.height);
            _playView.frame = newRect;
            _controlView.hidden = YES;
            _isFullScreen = YES;
        }];
        [UIView animateWithDuration:0.2 animations:^{
            _playView.transform = CGAffineTransformRotate(_playView.transform, M_PI/2);
            CGRect newRect = CGRectMake(0, 0, screenRect.size.width, screenRect.size.height- statusRect.size.height);
            _playView.frame = newRect;
            _controlView.hidden = YES;
            _isFullScreen = YES;
        }];
        [UIView animateWithDuration:0.2 animations:^{
            _playView.transform = CGAffineTransformRotate(_playView.transform, M_PI/2);
            CGRect newRect = CGRectMake(0, 0, screenRect.size.width, screenRect.size.height- statusRect.size.height);
            _playView.frame = newRect;
            _controlView.hidden = YES;
            _isFullScreen = YES;
        }];
    }
}

- (NSString *) getLocalRecordFilePath
{
    NSDateFormatter *dateformatter=[[NSDateFormatter alloc] init];
    [dateformatter setDateFormat:@"yyyy-MM-dd"];
    NSString* dateString = [dateformatter stringFromDate:[NSDate date]];
    NSString *fileName = [NSString stringWithFormat:@"%@.mp4",dateString];
    NSArray * docdirs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString * docdir = [docdirs objectAtIndex:0];
    NSString * recordFilePath = [docdir stringByAppendingPathComponent:@"record"];
    
    if(![[NSFileManager defaultManager] fileExistsAtPath:recordFilePath])
    {
        NSError *error = nil;
        [[NSFileManager defaultManager] createDirectoryAtPath:recordFilePath
                                  withIntermediateDirectories:YES
                                                   attributes:nil
                                                        error:&error];
    }
    
    NSString *destFilePath = [recordFilePath stringByAppendingPathComponent:fileName];
    
    return destFilePath;
}

#pragma mark - delegate

- (void)player:(EZPlayer *)player didPlayFailed:(NSError *) error
{
    NSLog(@"播放异常:%@",error);
    
    switch (error.code)
    {
        // 34错误特殊处理，重启播放器
        case 34:
        {
            [_player stopPlayback];
            [self preparePlayer:_activeFile :_activeFileTime];
            break;
        }
    }
}

- (void)player:(EZPlayer *)player didReceivedMessage:(NSInteger)messageCode{
    // 不管成功失败都关闭加载动画
    [self hideLoading];
    switch (messageCode)
    {
        case PLAYER_PLAYBACK_START:
        {
            [self setPlayStatus];
            break;
        }
            
        case PLAYER_PLAYBACK_FINISHED:
        {
            [self setStopStatus];
            break;
        }
            
        case PLAYER_PLAYBACK_PAUSE:
        {
            [self setStopStatus];
            break;
        }
    }
}

- (void)player:(EZPlayer *)player didReceivedDisplayHeight:(NSInteger) height displayWidth:(NSInteger) width
{
    
}

@end
