//
//  KMMapView.h
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

#import "KMCoordinateRegion.h"

@protocol KMMapLayer;
@protocol KMAnnotation;

@interface KMMapView : NSView {
@private
    id _delegate;
    id _baseRenderer;
    
    BOOL _mouseMoved;
    BOOL _passMouseEventsToDelegate;
    NSTrackingArea *_trackingArea;
}

#pragma mark - Delegate

@property (assign) id delegate;


#pragma mark - Zoom, Center & Region

@property (nonatomic, assign) double zoom;
@property (nonatomic, assign) CLLocationCoordinate2D center;
@property (nonatomic, assign) KMCoordinateRegion region;

- (void)setZoom:(double)level animated:(BOOL)animated;
- (void)setCenter:(CLLocationCoordinate2D)coordinate animated:(BOOL)animated;
- (void)setRegion:(KMCoordinateRegion)rect animated:(BOOL)animated;


#pragma mark - Base Layer

@property (readonly) id <KMMapLayer> baseLayer;


#pragma mark - Overlays

@property (readonly) NSArray *overlays;

- (void)addOverlay:(id <KMMapLayer>)layer;
- (void)addOverlays:(NSArray *)layers;

- (void)removeOverlay:(id <KMMapLayer>)layer;
- (void)removeOverlays:(NSArray *)layers;

- (void)insertOverlay:(id <KMMapLayer>)layer atIndex:(NSUInteger)index;
- (void)insertOverlay:(id <KMMapLayer>)layer aboveOverlay:(id <KMMapLayer>)sibling;
- (void)insertOverlay:(id <KMMapLayer>)layer belowOverlay:(id <KMMapLayer>)sibling;


#pragma mark - Annotaions

@property (readonly) NSArray *annotations;

- (void)addAnnotation:(id <KMAnnotation>)annotation;
- (void)addAnnotations:(NSArray *)annotations;

- (void)removeAnnotation:(id <KMAnnotation>)annotation;
- (void)removeAnnotations:(NSArray *)annotations;

@end




