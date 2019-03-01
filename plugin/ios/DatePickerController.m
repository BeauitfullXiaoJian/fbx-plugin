//
//  DatePickerController.m
//  督贝督导端
//
//  Created by  anasit on 2019/2/13.
//

#import "DatePickerController.h"

@interface DatePickerController()
@property (weak, nonatomic) IBOutlet UIDatePicker *dateView;

@end

@implementation DatePickerController

- (void)viewDidLoad
{
    [super viewDidLoad];
}

- (IBAction)confirmDate:(id)sender {
    [(PlaybackViewController *)_parentCtrl updatePickerDate:_identify :_dateView.date];
    [self dismissViewControllerAnimated:TRUE completion:nil];
}

@end

