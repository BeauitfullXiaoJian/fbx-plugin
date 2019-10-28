# 萤石云直播、回放Cordova插件

## 注意
这个项目为指定应用开发，请不要直接用于您的项目，建议只参考Cordova的萤石云SDK接入方法。

## 包含功能
1. 应用更新（Android）
2. 视频预览
3. 视频回放

## 安装命令
`cordova plugin add fbx-plugin`

## IOS安装问题
1. 需要把EZOpenSDKFramework.framework再次添加到Embedded Binaries中

## ANDROID安装问题
1. framework在cordova-android@~7.0.0报错,只能手动添加了
```xml
 <framework src="src/config.gradle" custom="true" type="gradleReference" />
```
在build
```
android.defaultConfig {
    vectorDrawables {
        useSupportLibrary true
    }
}
```

## 参考代码

### 回放
```js
window.FBX && window.FBX.call('playback', null, null, [
    // appKey-萤石云
    'zzzzzzz7e8fczzzzzz0791a21zzzzz',
    // accessToken-萤石云
    'at.fsd-3d2i76fdsfdffdsf6r61-fdsf-fdsffdfds',
    // cameraName-设备名称
    'C6HN(34354357038)',
    // cameraSeries-设备序列号
    '137403034',
    // cameraNo-设备通道号
    1
]);
```

### 直播
```js
 window.FBX && window.FBX.call('live', null, null, [
     // appKey-萤石云
     '1117e8fc982f40c88570791a2127230a',
     // accessToken-萤石云
     'at.c8ws6jvrdry6kt31ai4nq71ddwoa7a9m-35zayd8x20-1okae5n-pythgjpix',
     // 店铺相关数据
     JSON.stringify({ storeId: 629, storeTitle: '测试店铺' }),
     // 店铺摄像头列表数据
     JSON.stringify([
         { shopId: 1, cameraId: 1, cameraNo: 1, cameraSns: '116382128', cameraTitle: '大门口', online: true, snapshotUrl: 'https://picsum.photos/600/360?100' },
         { shopId: 1, cameraId: 2, cameraNo: 1, cameraSns: 'C39830868', cameraTitle: '杂物间', online: true, snapshotUrl: 'https://picsum.photos/600/360?200' },
     ]),
     // 上传地址
     '.......................',
     // 截图上传地址
     '/web/camera/photo/upload',
     // 表单提交地址
     '/web/check/table/saveGradeTableOnlineCheck',
     // 权限令牌数据
     '1',
     '1',
     'termName'
    ]);
```
