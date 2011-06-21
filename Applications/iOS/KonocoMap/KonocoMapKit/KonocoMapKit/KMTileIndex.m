//
//  KMTileIndex.m
//  KonocoMapKit
//
//  Created by Tobias Kräntzer on 28.05.11.
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

#import "KMTileIndex.h"


@implementation KMTileIndex

@synthesize zoom = _zoom;
@synthesize x = _x;
@synthesize y = _y;

+ (KMTileIndex *)indexWithZoom:(NSUInteger)zoom
                             x:(NSUInteger)x
                             y:(NSUInteger)y
{
    return [[[KMTileIndex alloc] initWithZoom:zoom x:x y:y] autorelease];
}

- (id)initWithZoom:(NSUInteger)zoom
                 x:(NSUInteger)x
                 y:(NSUInteger)y
{
    self = [super init];
    if (self) {
        _zoom = zoom;
        _x = x;
        _y = y;
    }
    return self;
}

#pragma mark -

- (BOOL)isEqual:(id)object
{
    if ([object isKindOfClass:[KMTileIndex class]]) {
        KMTileIndex *other = (KMTileIndex *)object;
        return _zoom == other.zoom && _x == other.x && _y == other.y;
    }
    return NO; 
}

#pragma mark -

- (NSString *)description
{
    return [NSString stringWithFormat:@"KMIndex {zoom:%d; x:%d; y:%d}", _zoom, _x, _y];
}

@end
