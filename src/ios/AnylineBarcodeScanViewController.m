
#import "AnylineBarcodeScanViewController.h"
#import <Anyline/Anyline.h>

@interface AnylineBarcodeScanViewController ()<AnylineBarcodeModuleDelegate>

@end

@implementation AnylineBarcodeScanViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    dispatch_async(dispatch_get_main_queue(), ^{
        AnylineBarcodeModuleView *barcodeModuleView = [[AnylineBarcodeModuleView alloc] initWithFrame:self.view.bounds];
        
        NSError *error = nil;
        [barcodeModuleView setupWithLicenseKey:self.key delegate:self error:&error];
//        if(!success) {
//            UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Setup failed:" message:error.debugDescription delegate:self cancelButtonTitle:@"Ok" otherButtonTitles:nil];
//            [alert show];
//        }

        barcodeModuleView.currentConfiguration = self.conf;

        self.moduleView = barcodeModuleView;
        
        [self.view addSubview:self.moduleView];
        
        [self.view sendSubviewToBack:self.moduleView];
    });
}

#pragma mark - AnylineBarcodeModuleDelegate method


- (void)anylineBarcodeModuleView:(AnylineBarcodeModuleView *)anylineBarcodeModuleView
                   didFindResult:(ALBarcodeResult *)scanResult {
    
    self.scannedLabel.text = (NSString *)scanResult.result;
    [self flashResultFor:0.9];
    
    NSMutableDictionary *dictResult = [NSMutableDictionary dictionaryWithCapacity:2];
    
    [dictResult setObject:(NSString *)scanResult.result forKey:@"value"];
     if (!scanResult.barcodeFormat) {
            [dictResult setObject:@"Unknown" forKey:@"barcodeFormat"];
        } else {
            [dictResult setObject:[self stringFromBarcodeFormat:scanResult.barcodeFormat] forKey:@"barcodeFormat"];
        }

    
    NSString *imagePath = [self saveImageToFileSystem:scanResult.image];
    
    [dictResult setValue:imagePath forKey:@"imagePath"];
    
    [dictResult setValue:@(scanResult.confidence) forKey:@"confidence"];
    [dictResult setValue:[self stringForOutline:scanResult.outline] forKey:@"outline"];
    
    [self.delegate anylineBaseScanViewController:self
                                         didScan:dictResult
                                continueScanning:!self.moduleView.cancelOnResult];
    if (self.moduleView.cancelOnResult) {
        [self dismissViewControllerAnimated:YES completion:NULL];
    }
}



#pragma mark - Private Methods

- (ALBarcodeFormat)barcodeFormatFromString:(NSString *)barcodeFormat {
    NSDictionary<NSString *, NSNumber *> *scanModes = [self barcodesFormatDict];
    
    return [scanModes[barcodeFormat] integerValue];
}

- (NSString *)stringFromBarcodeFormat:(ALBarcodeFormat)barcodeFormat {
    NSDictionary<NSString *, NSNumber *> *barcodeFormats = [self barcodesFormatDict];
    return [barcodeFormats allKeysForObject:@(barcodeFormat)][0];
}

- (NSDictionary<NSString *, NSNumber *> *)barcodesFormatDict {
    static NSDictionary<NSString *, NSNumber *> * scanModes = nil;
    
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        scanModes = @{
                      @"AZTEC" : @(ALCodeTypeAztec),
                      @"CODABAR" : @(ALCodeTypeCodabar),
                      @"CODE_39" : @(ALCodeTypeCode39),
                      @"CODE_93" : @(ALCodeTypeCode93),
                      @"CODE_128" : @(ALCodeTypeCode128),
                      @"DATA_MATRIX" : @(ALCodeTypeDataMatrix),
                      @"EAN_8" : @(ALCodeTypeEAN8),
                      @"EAN_13" : @(ALCodeTypeEAN13),
                      @"ITF" : @(ALCodeTypeITF),
                      @"PDF_417" : @(ALCodeTypePDF417),
                      @"QR_CODE" : @(ALCodeTypeQR),
                      @"RSS_14" : @(ALCodeTypeRSS14),
                      @"RSS_EXPANDED" : @(ALCodeTypeRSSExpanded),
                      @"UPC_A" : @(ALCodeTypeUPCA),
                      @"UPC_E" : @(ALCodeTypeUPCE),
                      @"UPC_EAN_EXTENSION" : @(ALCodeTypeUPCEANExtension),
                      @"UNKNOWN" : @(ALHeatMeter6),
                      };
    });
    
    return scanModes;
}



@end
