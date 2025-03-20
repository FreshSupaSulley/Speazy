#import "MicrophoneAccess.h"
#include <iostream>
#include <AVFoundation/AVFoundation.h>
#import <Cocoa/Cocoa.h>

const char* getMicAuthStatus() {
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeAudio];
    switch (status) {
        case AVAuthorizationStatusNotDetermined:
            // The user hasn't decided yet
            return "NotDetermined";
        case AVAuthorizationStatusAuthorized:
            // The user has authorized access
            return "Authorized";
        case AVAuthorizationStatusDenied:
            // The user has denied access
            return "Denied";
        case AVAuthorizationStatusRestricted:
            // Access is restricted (e.g., parental controls)
            return "Restricted";
        default:
            return "Unknown";
    }
}

bool requestMicrophoneAccess() {
    // Request microphone access if it hasn't been granted yet
    [AVCaptureDevice requestAccessForMediaType:AVMediaTypeAudio completionHandler:^(BOOL granted) {
        if (granted) {
            std::cout << "Microphone access granted!" << std::endl;
        } else {
            std::cout << "Microphone access denied." << std::endl;
        }
    }];
    return false;
}

void openPrivacySettings() {
    NSURL *url = [NSURL URLWithString:@"x-apple.systempreferences:com.apple.preference.security?Privacy_Microphone"];
    if (url) {
        [[NSWorkspace sharedWorkspace] openURL:url];
    }
}
