//
//  OSMLayer.m
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

#import "OSMLayer.h"

@interface OSMLayer ()
@property (nonatomic, readonly) NSString *applicationSupportDirectory;
@property (nonatomic, readonly) NSString *mapCacheDirectory;
@end


@implementation OSMLayer

- (id)init
{
    self = [super init];
    if (self) {
        // Initialization code here.
    }
    return self;
}

- (void)dealloc
{
    [super dealloc];
}

- (void)drawTile:(KMTileIndex *)idx inContext:(CGContextRef)ctx
{    
    NSString *tileFolder = [self.mapCacheDirectory stringByAppendingPathComponent:[NSString pathWithComponents:[NSArray arrayWithObjects:[NSString stringWithFormat:@"%d", idx.zoom],[NSString stringWithFormat:@"%d", idx.x], nil]]];
    NSString *localPath = [tileFolder stringByAppendingPathComponent:[NSString stringWithFormat:@"%d.png", idx.y]];
    
    NSFileManager *fileManager = [NSFileManager defaultManager];
    
    NSImage *tile;
    
    if ([fileManager fileExistsAtPath:localPath]) {
        tile = [[NSImage alloc] initWithContentsOfFile:localPath];
    } else {
        NSURL *tileURL = [NSURL URLWithString:[NSString stringWithFormat:@"%@/%d/%d/%d.png", @"http://tile.openstreetmap.org", idx.zoom, idx.x, idx.y]];
        NSData *tileData = [NSData dataWithContentsOfURL:tileURL];
        NSError *error;
        [fileManager createDirectoryAtPath:tileFolder withIntermediateDirectories:YES attributes:nil error:&error];
        [tileData writeToFile:localPath atomically:YES];
        tile = [[NSImage alloc] initWithData:tileData];
    }
    
    if (tile == nil)
        return;

    CGImageSourceRef source = CGImageSourceCreateWithData((CFDataRef)[tile TIFFRepresentation], NULL);
    CGImageRef maskRef =  CGImageSourceCreateImageAtIndex(source, 0, NULL);
    
    CGRect rect = CGContextGetClipBoundingBox(ctx);
    CGContextDrawImage(ctx, rect, maskRef);
    
    CGImageRelease(maskRef);
    CFRelease(source);
    
    [tile release];
    
    //CGContextSetLineWidth(ctx, 0.001/log(idx.zoom));
    //CGContextStrokeRect(ctx, rect);
}

- (NSString *)applicationSupportDirectory {
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES);
    NSString *basePath = ([paths count] > 0) ? [paths objectAtIndex:0] : NSTemporaryDirectory();
    return [basePath stringByAppendingPathComponent:@"KonocoMap"];
}

- (NSString *)mapCacheDirectory {
    return [self.applicationSupportDirectory stringByAppendingPathComponent:@"MapTileCacheOSM"];
}

@end
