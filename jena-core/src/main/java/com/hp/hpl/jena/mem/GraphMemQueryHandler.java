/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.hpl.jena.mem;

import com.hp.hpl.jena.graph.Triple ;
import com.hp.hpl.jena.graph.query.ExpressionSet ;
import com.hp.hpl.jena.graph.query.Mapping ;
import com.hp.hpl.jena.graph.query.QueryHandler ;
import com.hp.hpl.jena.graph.query.Stage ;

public class GraphMemQueryHandler extends GraphMemBaseQueryHandler implements QueryHandler
    {
    GraphMemQueryHandler( GraphMem graph ) 
        { super( graph ); }
    
    @Override public Stage patternStage( Mapping map, ExpressionSet constraints, Triple [] t )
        { return new PatternStageMem( graph, map, constraints, t ); }
    }