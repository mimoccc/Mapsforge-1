//
//  KonocoMapWindowController.m
//  KonocoMap
//
//  Created by Tobias Kräntzer on 07.04.10.
//  Copyright 2010 Konoco <http://konoco.org/> All rights reserved.
//
//  This file is part of Konoco Map.
//	
//  Map is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//	
//  Map is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public License
//  along with Map.  If not, see <http://www.gnu.org/licenses/>.
//

#import "KonocoMapWindowController.h"

#import <KonocoMapKit/KonocoMapKit.h>

@interface KonocoMapWindowController ()
- (void)setStyleBorderless;
- (void)setStyleNormal;
- (void)setFullScreen;
- (void)setWindowScreen;
@end

@implementation KonocoMapWindowController

@synthesize mapView;

- (id)init {
    self = [super initWithWindowNibName:@"KonocoMapWindow"];
	if (self) {
		[self.window setExcludedFromWindowsMenu:YES];
        self.mapView.delegate = self;
	}
	return self;
}

- (void)dealloc {
	[super dealloc];
}

#pragma mark -
#pragma mark Actions

- (IBAction)zoomIn:(id)sender
{
	[mapView setZoom:floor(mapView.zoom) + 1 animated:YES];
}

- (IBAction)zoomOut:(id)sender
{
	[mapView setZoom:floor(mapView.zoom) - 1 animated:YES];
}

- (IBAction)toggleFullscreen:(id)sender
{
    if (inFullScreenMode) {
        [self setWindowScreen];
        [self performSelector:@selector(setStyleNormal) 
                   withObject:nil
                   afterDelay:[self.window animationResizeTime:normalFrame]];
        inFullScreenMode = NO;
    } else {
        [self setStyleBorderless];
        [self performSelector:@selector(setFullScreen)
                   withObject:nil
                   afterDelay:[self.window animationResizeTime:[[NSScreen mainScreen] frame]]];
        inFullScreenMode = YES;
    }
}

#pragma mark - Fullscreen

- (void)setStyleBorderless {
    normalStyleMask = [self.window styleMask];
    [self.window setStyleMask:NSBorderlessWindowMask];
}

- (void)setStyleNormal {
    [self.window setStyleMask:normalStyleMask];
}

- (void)setFullScreen {
    normalFrame = [self.window frame];
    [self.window setLevel:CGShieldingWindowLevel()];
    [self.window setFrame:[[NSScreen mainScreen] frame]
                  display:YES
                  animate:YES];
}

- (void)setWindowScreen {
    [self.window setLevel:kCGNormalWindowLevel];
    [self.window setFrame:normalFrame
                  display:YES
                  animate:YES];
}

#pragma mark -

- (void)flagsChanged:(NSEvent *)event {
    if ([event modifierFlags] & NSAlternateKeyMask) {
        inEditMode = YES;
    } else {
        inEditMode = NO;
    }
}

#pragma mark - Delegate

- (void)mapView:(KMMapView *)mapView regionWillChangeAnimated:(BOOL)animated
{
//    NSLog(@"%s", __FUNCTION__);
}

- (void)mapView:(KMMapView *)mapView regionDidChangeAnimated:(BOOL)animated
{
//    NSLog(@"%s", __FUNCTION__);
}

- (void)mapView:(KMMapView *)mapView mouseClickAtCoordinate:(CLLocationCoordinate2D)coordinate
{
//    NSLog(@"%s", __FUNCTION__);
    
    id <KMAnnotation> annotation = [KMPointAnnotation annotationWithCoordinate:coordinate];
    
    [self.mapView addAnnotation:annotation];
    
    double delayInSeconds = 5.0;
    dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, delayInSeconds * NSEC_PER_SEC);
    dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
        [self.mapView removeAnnotation:annotation];
    });
}

- (void)mapView:(KMMapView *)mapView mouseMovedToCoordinate:(CLLocationCoordinate2D)coordinate
{
//    NSLog(@"%s", __FUNCTION__);
}

- (BOOL)respondToLeftMouseEventsForMapView:(KMMapView *)mapView
{
//    NSLog(@"%s", __FUNCTION__);
    return inEditMode;
}

- (void)mapView:(KMMapView *)mapView mouseDownAtCoordinate:(CLLocationCoordinate2D)coordinate withEvent:(NSEvent *)event
{
//    NSLog(@"%s", __FUNCTION__);    
}

- (void)mapView:(KMMapView *)mapView mouseDraggedToCoordinate:(CLLocationCoordinate2D)coordinate withEvent:(NSEvent *)event
{
//    NSLog(@"%s", __FUNCTION__);
}

- (void)mapView:(KMMapView *)mapView mouseUpAtCoordinate:(CLLocationCoordinate2D)coordinate withEvent:(NSEvent *)event
{
//    NSLog(@"%s", __FUNCTION__);
}

- (NSMenu *)mapView:(KMMapView *)mapView menuForCoordinate:(CLLocationCoordinate2D)coordinate withEvent:(NSEvent *)event
{
//    NSLog(@"%s", __FUNCTION__);
    NSMenu *theMenu = [[[NSMenu alloc] initWithTitle:@"Selected Coordinate"] autorelease];
    [theMenu insertItemWithTitle:[NSString stringWithFormat:@"Longitude: %f Latitude: %f", coordinate.longitude, coordinate.latitude] action:nil keyEquivalent:@"" atIndex:0];
    return theMenu;
}

- (KMAnnotationView *)mapView:(KMMapView *)mapView viewForAnnotation:(id <KMAnnotation>)annotation
{
//    NSLog(@"%s", __FUNCTION__);
    return [[[KMAnnotationView alloc] initWithFrame:NSRectFromCGRect(CGRectMake(0, 0, 20, 20))] autorelease];
}

@end
