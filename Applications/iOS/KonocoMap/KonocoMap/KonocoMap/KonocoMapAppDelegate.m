//
//  KonocoMapAppDelegate.m
//  KonocoMap
//
//  Created by Tobias Kräntzer on 27.05.11.
//  Copyright 2010, 2011 Konoco <http://konoco.org/> All rights reserved.
//
//  This file is part of KonocoMap.
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

#import "KonocoMapAppDelegate.h"
#import "KonocoMapWindowController.h"

#import <KonocoMapKit/KonocoMapKit.h>


@implementation KonocoMapAppDelegate

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{
    locationManager = [CLLocationManager new];
    locationManager.delegate = self;
    locationManager.distanceFilter = 1;
    locationManager.desiredAccuracy = 1;
    [locationManager startUpdatingLocation];
    
    [self showMapWindow:self];
}

- (void)dealloc
{
    [locationManager release];
    [mapWindowController release];
    [super dealloc];
}


#pragma mark -
#pragma mark Actions

- (IBAction)showMapWindow:(id)sender
{
	if (!mapWindowController) {
		mapWindowController = [KonocoMapWindowController new];
	}
	[mapWindowController showWindow:self];
	[mapWindowController.window makeKeyAndOrderFront:nil];
}

- (IBAction)zoomIn:(id)sender
{
	if (mapWindowController) {
		[mapWindowController zoomIn:self];
	}
}

- (IBAction)zoomOut:(id)sender
{
	if (mapWindowController) {
		[mapWindowController zoomOut:self];
	}
}

- (IBAction)toggleFullscreen:(id)sender
{
	if (mapWindowController) {
		[mapWindowController toggleFullscreen:sender];
	}
}


#pragma mark -
#pragma mark Location Manager Delegate Methods

- (void)locationManager:(CLLocationManager *)manager
       didFailWithError:(NSError *)error {
    NSLog(@"Location manager did fail with error: %@", [error localizedDescription]);
}

- (void)locationManager:(CLLocationManager *)manager
    didUpdateToLocation:(CLLocation *)newLocation
           fromLocation:(CLLocation *)oldLocation {

    [mapWindowController.mapView setZoom:10 animated:YES];
    [mapWindowController.mapView setCenter:newLocation.coordinate animated:YES];
}

@end
