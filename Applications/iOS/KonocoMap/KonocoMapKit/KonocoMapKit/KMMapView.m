//
//  KMMapView.m
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

#import "KMMapView.h"

#import "KMMapViewDelegate.h"
#import "KMMapLayer.h"
#import "KMTiledRenderer.h"
#import "KMTileIndex.h"

#import "OSMLayer.h"

#import "KMAnnotation.h"
#import "KMAnnotationView.h"

typedef struct {
    double x, y;
} KMMapPoint;

typedef double KMMapScale;

typedef struct {
    double deltaX, deltaY;
} KMMapSpan;

typedef struct {
    KMMapPoint center;
    KMMapSpan span;
} KMMapRegion;

KMMapPoint KMMapPointMake(double x, double y);

CLLocationCoordinate2D coordinateFromMapPoint(KMMapPoint mapPoint);
KMMapPoint mapPointFromCoordinate(CLLocationCoordinate2D coordinate);

KMCoordinateRegion coordinateRegionFromMapRegion(KMMapRegion mapRegion);
KMMapRegion mapRegionFromCoordinateRegion(KMCoordinateRegion coordinateRegion);

@interface KMMapView ()

@property (readonly) KMTiledRenderer *baseRenderer;

- (CLLocationCoordinate2D)coordinateForMouseLocation:(NSPoint)eventLocation;

#pragma mark - Visible Region

@property (assign) KMMapScale mapScale;
@property (assign) KMMapPoint mapCenter;

- (void)setMapCenter:(KMMapPoint)center
           withScale:(KMMapScale)scale
            animated:(BOOL)animated
     completionBlock:(void (^)(void))block;

@end






@implementation KMMapView

@synthesize delegate = _delegate;

#pragma mark - Life Cycle

- (id)initWithFrame:(NSRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code here.
        [self setMapCenter:KMMapPointMake(0.5, 0.5)
                 withScale:1
                  animated:NO
           completionBlock:nil];
    }
    return self;
}

- (void)dealloc
{
    [_baseRenderer release];
    [_trackingArea release];
    [super dealloc];
}

#pragma mark -

- (void)awakeFromNib
{
    [self setMapCenter:KMMapPointMake(0.5, 0.5)
             withScale:1
              animated:NO
       completionBlock:nil];
}

#pragma mark -

#pragma mark - Zoom

- (double)zoom
{
    return log2f(self.mapScale);
}

- (void)setZoom:(double)aZoom
{
    return [self setZoom:aZoom
                animated:NO];
}

- (void)setZoom:(double)level
       animated:(BOOL)animated
{
    [self setMapCenter:self.mapCenter
             withScale:powf(2, level)
              animated:animated
       completionBlock:nil];
}

#pragma mark - Center

- (CLLocationCoordinate2D)center
{
    return coordinateFromMapPoint(self.mapCenter);
}

- (void)setCenter:(CLLocationCoordinate2D)aCenter
{
    return [self setCenter:aCenter
                  animated:NO];
}

- (void)setCenter:(CLLocationCoordinate2D)coordinate
         animated:(BOOL)animated
{
    KMMapPoint point = mapPointFromCoordinate(coordinate);
    
    [self setMapCenter:point
             withScale:self.mapScale
              animated:animated
       completionBlock:nil];
}

#pragma mark - Region

- (KMCoordinateRegion)region
{
    KMMapScale scale = self.mapScale;
	
	double width = self.bounds.size.width / (self.baseRenderer.tileSize.width * scale);
	double height = self.bounds.size.height / (self.baseRenderer.tileSize.height * scale);
    
    KMMapRegion mapRegion;
    mapRegion.center = self.mapCenter;
    mapRegion.span.deltaX = width;
    mapRegion.span.deltaY = height;
    
	return coordinateRegionFromMapRegion(mapRegion);
}

- (void)setRegion:(KMCoordinateRegion)aRegion
{
    return [self setRegion:aRegion
                  animated:NO];
}

- (void)setRegion:(KMCoordinateRegion)aRegion
         animated:(BOOL)animated
{
    KMMapRegion mapRegion = mapRegionFromCoordinateRegion(aRegion);
    
    KMMapScale mapScale = self.bounds.size.width * mapRegion.span.deltaX / self.baseRenderer.tileSize.width;
    
    [self setMapCenter:mapRegion.center
             withScale:mapScale
              animated:animated
       completionBlock:nil];
}

#pragma mark - Base Layer

- (id <KMMapLayer>)baseLayer
{
    return self.baseRenderer.mapLayer;
}

#pragma mark - Overlays

- (NSArray *)overlays
{
    NSMutableArray *result = [NSMutableArray array];
    for (KMTiledRenderer *overlay in self.baseRenderer.sublayers) {
        if ([overlay isKindOfClass:[KMTiledRenderer class]]) {
            [result addObject:overlay.mapLayer];
        }
    }
    return result;
}

- (void)addOverlay:(id <KMMapLayer>)layer
{
    return [self addOverlays:[NSArray arrayWithObject:layer]];
}

- (void)addOverlays:(NSArray *)layers
{
    for (id <KMMapLayer> layer in layers) {
        KMTiledRenderer *overlay = [KMTiledRenderer layer];
        overlay.bounds = self.baseRenderer.bounds;
        overlay.position = CGPointMake(self.baseRenderer.bounds.size.width / 2,
                                       self.baseRenderer.bounds.size.height / 2);
        
        overlay.masksToBounds = NO;
        overlay.levelsOfDetail = 17;
        overlay.levelsOfDetailBias = 17;
        overlay.opaque = NO;
        overlay.mapLayer = layer;
        overlay.delegate = self;
        [self.baseRenderer addSublayer:overlay];
    }
}

- (void)removeOverlay:(id <KMMapLayer>)layer
{
    return [self removeOverlays:[NSArray arrayWithObject:layer]];
}

- (void)removeOverlays:(NSArray *)layers
{
    NSArray *sublayers = [self.baseRenderer.sublayers copy];
    for (KMTiledRenderer *overlay in sublayers) {
        if ([overlay isKindOfClass:[KMTiledRenderer class]]) {
            if ([layers containsObject:overlay.mapLayer]) {
                [overlay removeFromSuperlayer];
            }
        }
    }
    [sublayers release];
}

- (void)insertOverlay:(id <KMMapLayer>)layer
              atIndex:(NSUInteger)index
{
    // TODO: Insert overlay at index.
    [[NSException exceptionWithName:@"Not Implemented" reason:@"" userInfo:nil] raise];
}

- (void)insertOverlay:(id <KMMapLayer>)layer
         aboveOverlay:(id <KMMapLayer>)sibling
{
    // TODO: Insert overlay above overlay.
    [[NSException exceptionWithName:@"Not Implemented" reason:@"" userInfo:nil] raise];

}

- (void)insertOverlay:(id <KMMapLayer>)layer
         belowOverlay:(id <KMMapLayer>)sibling
{
    // TODO: Insert overlay below overlay.
    [[NSException exceptionWithName:@"Not Implemented" reason:@"" userInfo:nil] raise];

}

#pragma mark - Annotations

- (NSArray *)annotations
{
    NSMutableArray *result = [NSMutableArray array];
    for (id v in [self subviews]) {
        if ([v isKindOfClass:[KMAnnotationView class]]) {
            [result addObject:[(KMAnnotationView *)v annotation]];
        }
    }
    return result;
}

- (void)addAnnotation:(id <KMAnnotation>)annotation
{    
    return [self addAnnotations:[NSArray arrayWithObject:annotation]];
}

- (void)addAnnotations:(NSArray *)annotations
{
    if ([_delegate respondsToSelector:@selector(mapView:viewForAnnotation:)]) {
        for (id <KMAnnotation> annotation in annotations) {
            KMAnnotationView *annotationView = [_delegate mapView:self viewForAnnotation:annotation];
            if (annotationView) {
                annotationView.annotation = annotation;
                
                KMMapPoint mapPoint = mapPointFromCoordinate(annotation.coordinate);
                CGPoint originPoint = CGPointMake(mapPoint.x *self.baseRenderer.tileSize.width,
                                                  mapPoint.y * self.baseRenderer.tileSize.height);
                CGPoint layerPoint = [self.layer convertPoint:originPoint
                                                    fromLayer:self.baseRenderer];
                NSPoint localPoint = [self convertPoint:NSPointFromCGPoint(layerPoint)
                                               fromView:nil];
                localPoint.x -= annotationView.bounds.size.width / 2;
                localPoint.y -= annotationView.bounds.size.height / 2;
                [annotationView setFrameOrigin:(NSPoint)localPoint];
                
                [self addSubview:annotationView];
            }
        }
    }
}

- (void)removeAnnotation:(id <KMAnnotation>)annotation
{
    return [self removeAnnotations:[NSArray arrayWithObject:annotation]];
}

- (void)removeAnnotations:(NSArray *)annotations
{
    NSArray *subviews = [[self subviews] copy];
    
    for (KMAnnotationView *view in subviews) {
        if ([view isKindOfClass:[KMAnnotationView class]]) {
            if ([annotations containsObject:view.annotation]) {
                [view removeFromSuperview];
            }
        }
    }
    [subviews release];
}

#pragma mark -

#pragma mark - Base Renderer

- (KMTiledRenderer *)baseRenderer
{
    if (_baseRenderer == nil) {
        [self setWantsLayer:YES];
        KMTiledRenderer *baseRenderer = [KMTiledRenderer new];
        _baseRenderer = baseRenderer;
        baseRenderer.bounds = CGRectMake(0, 0, 256, 256);
        baseRenderer.position = CGPointMake(self.bounds.size.width / 2,
                                          self.bounds.size.height / 2);
        baseRenderer.masksToBounds = NO;
        baseRenderer.levelsOfDetail = 18;
        baseRenderer.levelsOfDetailBias = 18;
        baseRenderer.delegate = self;
        baseRenderer.mapLayer = [[OSMLayer new] autorelease];
        [self.layer addSublayer:_baseRenderer];
    }
    return _baseRenderer;
}

- (void)drawLayer:(CALayer *)layer inContext:(CGContextRef)ctx
{
    if (![layer isKindOfClass:[KMTiledRenderer class]])
        return;
    
    KMTiledRenderer *kmLayer = (KMTiledRenderer *)layer;
    
    CGAffineTransform transform = CGContextGetCTM(ctx);
    
    NSUInteger z = log2(transform.a);
    NSUInteger x = -transform.tx / self.baseRenderer.tileSize.width;
    NSUInteger y = pow(2, z) + (transform.ty / self.baseRenderer.tileSize.height) - 1;
        
    KMTileIndex *idx = [[KMTileIndex alloc] initWithZoom:z x:x y:y];
    
    [kmLayer.mapLayer drawTile:idx inContext:ctx];
    
    [idx release];
}

#pragma mark - Change Visible Region

- (void)setMapCenter:(KMMapPoint)newCenter
           withScale:(KMMapScale)newScale
            animated:(BOOL)animated
     completionBlock:(void (^)(void))block
{
    if ([_delegate respondsToSelector:@selector(mapView:regionWillChangeAnimated:)]) {
        [_delegate mapView:self regionWillChangeAnimated:animated];
    }
    
    if (!animated) {
		[CATransaction setValue:(id)kCFBooleanTrue
						 forKey:kCATransactionDisableActions];
	} else {
        
        for (id v in [self subviews]) {
            if ([v isKindOfClass:[KMAnnotationView class]]) {
                KMAnnotationView *annotationView = v;
                [annotationView setHidden:YES];
            }
        }
        
        [CATransaction setCompletionBlock:^{
            if ([_delegate respondsToSelector:@selector(mapView:regionDidChangeAnimated:)]) {
                [_delegate mapView:self regionDidChangeAnimated:animated];
            }
            
            for (id v in [self subviews]) {
                if ([v isKindOfClass:[KMAnnotationView class]]) {
                    KMAnnotationView *annotationView = v;
                    [annotationView setHidden:NO];
                }
            }
            
            if (block != nil)
                block();
        }];
    }
    
    double minScale = MAX(self.bounds.size.height / self.baseRenderer.tileSize.height,
						   self.bounds.size.width / self.baseRenderer.tileSize.width);
    
    newScale = MAX(newScale, minScale);
    self.mapScale = newScale;
    
	double marginX = self.bounds.size.width / 2 / (newScale *  self.baseRenderer.tileSize.width);
	double marginY = self.bounds.size.height / 2 / (newScale *  self.baseRenderer.tileSize.height);
    
	self.mapCenter = KMMapPointMake(MAX(MIN(newCenter.x, 1 - marginX), 0 + marginX),
                                    MAX(MIN(newCenter.y, 1 - marginY), 0 + marginY));
    
    
    for (id v in [self subviews]) {
        if ([v isKindOfClass:[KMAnnotationView class]]) {
            
            KMAnnotationView *annotationView = v;
            id <KMAnnotation> annotation = annotationView.annotation;
            
            KMMapPoint mapPoint = mapPointFromCoordinate(annotation.coordinate);
            CGPoint originPoint = CGPointMake(mapPoint.x *self.baseRenderer.tileSize.width,
                                              mapPoint.y * self.baseRenderer.tileSize.height);
            CGPoint layerPoint = [self.layer convertPoint:originPoint
                                                fromLayer:self.baseRenderer];
            NSPoint localPoint = [self convertPoint:NSPointFromCGPoint(layerPoint)
                                           fromView:nil];
            localPoint.x -= annotationView.bounds.size.width / 2;
            localPoint.y -= annotationView.bounds.size.height / 2;
            [annotationView setFrameOrigin:localPoint];
        }
    }
    
    if (!animated) {
        
        if ([_delegate respondsToSelector:@selector(mapView:regionDidChangeAnimated:)]) {
            [_delegate mapView:self regionDidChangeAnimated:animated];
        }
        
        if (block != nil)
            block();
    }
}

#pragma mark - Change Center & Scale

- (KMMapScale)mapScale
{
    CGAffineTransform aTransform = self.baseRenderer.affineTransform;
	return aTransform.a;
}

- (void)setMapScale:(KMMapScale)scale
{
    CGAffineTransform aTransform = CGAffineTransformIdentity;
	aTransform = CGAffineTransformScale(aTransform, scale, scale);
	self.baseRenderer.affineTransform = aTransform;
}

- (KMMapPoint)mapCenter
{
    CGPoint p = self.baseRenderer.anchorPoint;
    return KMMapPointMake(p.x, p.y);
}

- (void)setMapCenter:(KMMapPoint)aPoint
{
    CGPoint p = CGPointMake(aPoint.x, aPoint.y);
    self.baseRenderer.anchorPoint = p;
}

#pragma mark - View Management

- (void)setFrame:(NSRect)frameRect
{
	[super setFrame:frameRect];
	
    // The animation has to be disabled befor the position is set,
    // because this function is called for each 'step' if this view
    // changes its size.
    [CATransaction setValue:(id)kCFBooleanTrue
                     forKey:kCATransactionDisableActions];
    
    self.baseRenderer.position = CGPointMake(self.bounds.size.width / 2,
                                          self.bounds.size.height / 2);
	
    // Reset the scale and center of the map
    [self setMapCenter:self.mapCenter
             withScale:self.mapScale
              animated:NO
       completionBlock:nil];
}

- (void)viewWillDraw
{
    if (self.layer.sublayers == nil || [self.layer.sublayers indexOfObject:self.baseRenderer] == NSNotFound) {
        [self.layer addSublayer:self.baseRenderer];
    }
}

#pragma mark - Mouse Event Handling

- (CLLocationCoordinate2D)coordinateForMouseLocation:(NSPoint)eventLocation
{
    CGPoint layerPoint = [self.layer convertPoint:NSPointToCGPoint(eventLocation)
                                          toLayer:self.baseRenderer];
    
    return coordinateFromMapPoint(KMMapPointMake(layerPoint.x / self.baseRenderer.tileSize.width,
                                                 layerPoint.y / self.baseRenderer.tileSize.height));
}

- (void)mouseDown:(NSEvent *)event
{
    _mouseMoved = NO;
    
    if ([_delegate respondsToSelector:@selector(respondToLeftMouseEventsForMapView:)]) {
        _passMouseEventsToDelegate = [_delegate respondToLeftMouseEventsForMapView:self];
        
        if (_passMouseEventsToDelegate && [_delegate respondsToSelector:@selector(mapView:mouseDownAtCoordinate:withEvent:)]) {
            [_delegate mapView:self
         mouseDownAtCoordinate:[self coordinateForMouseLocation:[event locationInWindow]]
                     withEvent:event];
        }
        
    } else {
        _passMouseEventsToDelegate = NO;
    }
}

- (void)mouseDragged:(NSEvent *)event
{
    if (_passMouseEventsToDelegate) {
        if ([_delegate respondsToSelector:@selector(mapView:mouseDraggedToCoordinate:withEvent:)]) {
            [_delegate mapView:self
      mouseDraggedToCoordinate:[self coordinateForMouseLocation:[event locationInWindow]]
                     withEvent:event];
        }
    } else {
        double scale = self.mapScale;
        
        double deltaX = [event deltaX];
        double deltaY = [event deltaY];
        
        KMMapPoint currentCenter = self.mapCenter;
        
        KMMapPoint point = KMMapPointMake(currentCenter.x - deltaX / (scale * self.baseRenderer.tileSize.width),
                                          currentCenter.y + deltaY / (scale * self.baseRenderer.tileSize.height));
        [self setMapCenter:point
                 withScale:scale
                  animated:NO
           completionBlock:nil];
        
        _mouseMoved = YES;
    }
}

- (void)mouseUp:(NSEvent *)event
{
    if (_passMouseEventsToDelegate) {
        if ([_delegate respondsToSelector:@selector(mapView:mouseUpAtCoordinate:withEvent:)]) {
            [_delegate mapView:self
           mouseUpAtCoordinate:[self coordinateForMouseLocation:[event locationInWindow]]
                     withEvent:event];
        }
    } else {
        if (!_mouseMoved) {
            if ([_delegate respondsToSelector:@selector(mapView:mouseClickAtCoordinate:)]) {
                [_delegate mapView:self mouseClickAtCoordinate:[self coordinateForMouseLocation:[event locationInWindow]]];
            }
        }
    }
}

- (void)mouseMoved:(NSEvent *)event
{
    [NSObject cancelPreviousPerformRequestsWithTarget:self
                                             selector:@selector(hideCursor)
                                               object:nil];
    
    if ([_delegate respondsToSelector:@selector(mapView:mouseMovedToCoordinate:)]) {
        [_delegate mapView:self mouseMovedToCoordinate:[self coordinateForMouseLocation:[event locationInWindow]]];
    }
    
    [self performSelector:@selector(hideCursor)
               withObject:nil
               afterDelay:2];
}

- (void)hideCursor
{
    [NSCursor setHiddenUntilMouseMoves:YES];
}

- (void)updateTrackingAreas
{
    [self removeTrackingArea:_trackingArea];
    [_trackingArea release];
    _trackingArea = [[NSTrackingArea alloc] initWithRect:self.bounds
                                                 options:(NSTrackingMouseMoved | NSTrackingActiveInKeyWindow)
                                                   owner:self
                                                userInfo:nil];
    [self addTrackingArea:_trackingArea];
}

#pragma mark - Handling Scroll Wheel & Magnify Gesture

- (void)magnifyWithEvent:(NSEvent *)event
{
    [self setMapCenter:self.mapCenter
             withScale:powf(2, log2f(self.mapScale) + [event magnification])
              animated:NO
       completionBlock:nil];
}

- (void)scrollWheel:(NSEvent *)event
{
    double deltaX = -[event deltaX];
    double deltaY = -[event deltaY];
    
    if (fabs(deltaX) > 0 || fabs(deltaY) > 0) {
        
        double scale = self.mapScale;
        KMMapPoint currentCenter = self.mapCenter;
        KMMapPoint point = KMMapPointMake(currentCenter.x - deltaX / (scale * self.baseRenderer.tileSize.width),
                                          currentCenter.y + deltaY / (scale * self.baseRenderer.tileSize.height));
        
        [self setMapCenter:point
                 withScale:scale
                  animated:NO
           completionBlock:nil];
    }
}

#pragma mark - Context Menu for Coordinate

- (NSMenu *)menuForEvent:(NSEvent *)event
{
    if ([_delegate respondsToSelector:@selector(mapView:menuForCoordinate:withEvent:)]) {
        return [_delegate mapView:self
                menuForCoordinate:[self coordinateForMouseLocation:[event locationInWindow]]
                        withEvent:event];
    } else {
        return [super menuForEvent:event];
    }
}

@end

KMMapPoint
KMMapPointMake(double x, double y)
{
    KMMapPoint result;
    result.x = x;
    result.y = y;
    return result;
}

CLLocationCoordinate2D
coordinateFromMapPoint(KMMapPoint mapPoint)
{
    // http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#C.2FC.2B.2B
    
    CLLocationCoordinate2D result;
    result.longitude = mapPoint.x * 360.0 - 180.0;
    
    double n = M_PI - 2.0 * M_PI * (1 - mapPoint.y);
    result.latitude = 180.0 / M_PI * atan(0.5 * (exp(n) - exp(-n)));
    
    return result;
}

KMMapPoint
mapPointFromCoordinate(CLLocationCoordinate2D coordinate)
{
    // http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#C.2FC.2B.2B
    
    KMMapPoint result;
    result.x = (coordinate.longitude + 180.0) / 360.0;
    result.y = 1.0 - ((1.0 - log(tan(coordinate.latitude * M_PI/180.0) + 1.0 / cos(coordinate.latitude * M_PI/180.0)) / M_PI) / 2.0);
    return result;
}

KMCoordinateRegion
coordinateRegionFromMapRegion(KMMapRegion mapRegion)
{
    CLLocationCoordinate2D swCorner = coordinateFromMapPoint(KMMapPointMake(mapRegion.center.x - mapRegion.span.deltaX/2,
                                                                            mapRegion.center.y - mapRegion.span.deltaY/2));
    
    CLLocationCoordinate2D neCorner = coordinateFromMapPoint(KMMapPointMake(mapRegion.center.x + mapRegion.span.deltaX/2,
                                                                            mapRegion.center.y + mapRegion.span.deltaY/2));
    
    KMCoordinateRegion result;
    
    result.center.longitude = (swCorner.longitude + neCorner.longitude) / 2;
    result.center.latitude = (swCorner.latitude + neCorner.latitude) / 2;
    
    result.span.latitudeDelta = fabs(swCorner.latitude - neCorner.latitude);
    result.span.longitudeDelta = fabs(swCorner.longitude - neCorner.longitude);
    
    return result;
}

KMMapRegion
mapRegionFromCoordinateRegion(KMCoordinateRegion coordinateRegion)
{
    CLLocationCoordinate2D coordinate;
    
    coordinate.latitude = coordinateRegion.center.latitude - coordinateRegion.span.latitudeDelta/2;
    coordinate.longitude = coordinateRegion.center.longitude - coordinateRegion.span.longitudeDelta/2;
    KMMapPoint swCorner = mapPointFromCoordinate(coordinate);
    
    coordinate.latitude = coordinateRegion.center.latitude + coordinateRegion.span.latitudeDelta/2;
    coordinate.longitude = coordinateRegion.center.longitude + coordinateRegion.span.longitudeDelta/2;
    KMMapPoint neCorner = mapPointFromCoordinate(coordinate);
    
    KMMapRegion result;
    
    result.center.x = (swCorner.x + neCorner.x) / 2;
    result.center.y = (swCorner.y + neCorner.y) / 2;
    
    result.span.deltaX = fabs(swCorner.x - neCorner.x);
    result.span.deltaY = fabs(swCorner.y - neCorner.y);
    
    return result;
}





