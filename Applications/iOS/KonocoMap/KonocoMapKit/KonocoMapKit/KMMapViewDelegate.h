//
//  KMMapViewDelegate.h
//  KonocoMapKit
//
//  Created by Tobias Kräntzer on 07.04.10.
//  Copyright 2010, 2011 Konoco <http://konoco.org/> All rights reserved.
//
//  This file is part of KonocoMapKit.
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

#import <Cocoa/Cocoa.h>
#import <CoreLocation/CoreLocation.h>


@class KMMapView;
@class KMAnnotationView;

@protocol KMMapLayer;
@protocol KMMapRenderer;

@protocol KMMapViewDelegate <NSObject>

- (void)mapView:(KMMapView *)mapView regionWillChangeAnimated:(BOOL)animated;
- (void)mapView:(KMMapView *)mapView regionDidChangeAnimated:(BOOL)animated;

- (void)mapView:(KMMapView *)mapView mouseClickAtCoordinate:(CLLocationCoordinate2D)coordinate;
- (void)mapView:(KMMapView *)mapView mouseMovedToCoordinate:(CLLocationCoordinate2D)coordinate;

- (BOOL)respondToLeftMouseEventsForMapView:(KMMapView *)mapView;
- (void)mapView:(KMMapView *)mapView mouseDownAtCoordinate:(CLLocationCoordinate2D)coordinate withEvent:(NSEvent *)event;
- (void)mapView:(KMMapView *)mapView mouseDraggedToCoordinate:(CLLocationCoordinate2D)coordinate withEvent:(NSEvent *)event;
- (void)mapView:(KMMapView *)mapView mouseUpAtCoordinate:(CLLocationCoordinate2D)coordinate withEvent:(NSEvent *)event;

- (NSMenu *)mapView:(KMMapView *)mapView menuForCoordinate:(CLLocationCoordinate2D)coordinate withEvent:(NSEvent *)event; 

- (KMAnnotationView *)mapView:(KMMapView *)mapView viewForAnnotation:(id <KMAnnotation>)annotation;

@end
