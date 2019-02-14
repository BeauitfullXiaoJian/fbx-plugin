# DDXX 视频支持 涂鸦

## 相关说明
1. 应用更新（Android）
2. 视频预览
3. 视频回放

## 安装命令
`cordova plugin add fbx-plugin`

## IOS安装问题
1. 需要把EZOpenSDKFramework.framework再次添加到Embedded Binaries中

## 参考代码
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
