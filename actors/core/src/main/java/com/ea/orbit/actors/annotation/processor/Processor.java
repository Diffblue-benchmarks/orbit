/*
 Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ea.orbit.actors.annotation.processor;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.ActorObserver;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import java.io.PrintWriter;
import java.util.Set;

/**
 * Annotation processor to write class info for ActorObservers and Actors
 * This info speeds up classId lookup.
 */
@SupportedAnnotationTypes({"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Processor extends AbstractProcessor
{

    private Types typeUtils;
    private Elements elementUtils;
    private TypeMirror actorTypeMirror;
    private TypeMirror actorObserverTypeMirror;

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv)
    {
        roundEnv.getRootElements().forEach(this::processElement);
        return false;
    }

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        actorTypeMirror = elementUtils.getTypeElement(Actor.class.getName()).asType();
        actorObserverTypeMirror = elementUtils.getTypeElement(ActorObserver.class.getName()).asType();
    }

    void processElement(Element e)
    {
        if (e instanceof TypeElement)
        {
            final TypeMirror t1 = e.asType();
            if (typeUtils.isSubtype(t1, actorTypeMirror) || typeUtils.isSubtype(t1, actorObserverTypeMirror))
            {
                writeClassInfo((TypeElement) e);
            }
        }
        e.getEnclosedElements().forEach(this::processElement);
    }

    private void writeClassInfo(final TypeElement e)
    {
        try
        {
            final FileObject resource = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, "",
                    "META-INF/services/orbit/classes/" + elementUtils.getBinaryName(e) + ".yml", e);
            try (final PrintWriter writer = new PrintWriter(resource.openWriter()))
            {
                writer.append("classId: ").println(getClassId(e));
                writer.append("isActor: ").println(typeUtils.isSubtype(e.asType(), actorTypeMirror));
                writer.append("isObserver: ").println(typeUtils.isSubtype(e.asType(), actorObserverTypeMirror));
                writer.flush();
                writer.close();
            }
        }
        catch (Exception e1)
        {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error writing orbit class info for: " + e.getQualifiedName() + " error: " + e1, e);
        }
    }

    private int getClassId(final TypeElement e)
    {
        return elementUtils.getBinaryName(e).toString().hashCode();
    }
}
