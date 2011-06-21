//
//  KMPointAnnotation.h
//  KonocoMapKit
//
//  Created by Tobias Kräntzer on 03.06.11.
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

#import <Foundation/Foundation.h>
#import "KMAnnotation.h"

@interface KMPointAnnotation : NSObject <KMAnnotation> {
@private
    CLLocationCoordinate2D _coordinate;
}

+ (KMPointAnnotation *)annotationWithCoordinate:(CLLocationCoordinate2D)coordinate;
- (id)initWithCoordinate:(CLLocationCoordinate2D)coordinate;

@end
