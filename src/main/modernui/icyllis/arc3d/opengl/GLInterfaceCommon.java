/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.opengl;

import org.lwjgl.system.NativeType;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * OpenGL 3.1 Core and OpenGL ES 3.0 have a common subset.
 * <p>
 * No javadoc here, please refer to LWJGL javadoc and OpenGL specification.
 */
public interface GLInterfaceCommon {

    void glEnable(@NativeType("GLenum") int cap);

    void glDisable(@NativeType("GLenum") int cap);

    void glFrontFace(@NativeType("GLenum") int mode);

    void glLineWidth(@NativeType("GLfloat") float width);

    @NativeType("void")
    int glGenTextures();

    void glTexParameteri(@NativeType("GLenum") int target, @NativeType("GLenum") int pname,
                         @NativeType("GLint") int param);

    void glTexParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname,
                          @NativeType("GLint const *") IntBuffer params);

    void glTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level,
                      @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width,
                      @NativeType("GLsizei") int height, @NativeType("GLint") int border,
                      @NativeType("GLenum") int format, @NativeType("GLenum") int type,
                      @NativeType("void const *") long pixels);

    void glTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level,
                         @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset,
                         @NativeType("GLsizei") int width, @NativeType("GLsizei") int height,
                         @NativeType("GLenum") int format, @NativeType("GLenum") int type,
                         @NativeType("void const *") long pixels);

    void glCopyTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level,
                             @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset,
                             @NativeType("GLint") int x, @NativeType("GLint") int y,
                             @NativeType("GLsizei") int width, @NativeType("GLsizei") int height);

    void glDeleteTextures(@NativeType("GLuint const *") int texture);

    void glBindTexture(@NativeType("GLenum") int target, @NativeType("GLuint") int texture);

    void glPixelStorei(@NativeType("GLenum") int pname, @NativeType("GLint") int param);

    void glBlendFunc(@NativeType("GLenum") int sfactor, @NativeType("GLenum") int dfactor);

    void glColorMask(@NativeType("GLboolean") boolean red, @NativeType("GLboolean") boolean green,
                     @NativeType("GLboolean") boolean blue, @NativeType("GLboolean") boolean alpha);

    void glDepthFunc(@NativeType("GLenum") int func);

    void glDepthMask(@NativeType("GLboolean") boolean flag);

    void glStencilOp(@NativeType("GLenum") int sfail, @NativeType("GLenum") int dpfail,
                     @NativeType("GLenum") int dppass);

    void glStencilFunc(@NativeType("GLenum") int func, @NativeType("GLint") int ref, @NativeType("GLuint") int mask);

    void glStencilMask(@NativeType("GLuint") int mask);

    void glDrawArrays(@NativeType("GLenum") int mode, @NativeType("GLint") int first, @NativeType("GLsizei") int count);

    void glDrawElements(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count,
                        @NativeType("GLenum") int type, @NativeType("void const *") long indices);

    void glFlush();

    void glFinish();

    @NativeType("GLenum")
    int glGetError();

    @Nullable
    @NativeType("GLubyte const *")
    String glGetString(@NativeType("GLenum") int name);

    @NativeType("void")
    int glGetInteger(@NativeType("GLenum") int pname);

    void glScissor(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width,
                   @NativeType("GLsizei") int height);

    void glViewport(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width,
                    @NativeType("GLsizei") int height);

    void glActiveTexture(@NativeType("GLenum") int texture);

    void glBlendEquation(@NativeType("GLenum") int mode);

    @NativeType("void")
    int glGenBuffers();

    void glDeleteBuffers(@NativeType("GLuint const *") int buffer);

    void glBindBuffer(@NativeType("GLenum") int target, @NativeType("GLuint") int buffer);

    void glBufferData(@NativeType("GLenum") int target, @NativeType("GLsizeiptr") long size,
                      @NativeType("void const *") long data, @NativeType("GLenum") int usage);

    void glBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset,
                         @NativeType("GLsizeiptr") long size, @NativeType("void const *") long data);

    @NativeType("GLboolean")
    boolean glUnmapBuffer(@NativeType("GLenum") int target);

    void glDrawBuffers(@NativeType("GLenum const *") int[] bufs);

    void glStencilOpSeparate(@NativeType("GLenum") int face, @NativeType("GLenum") int sfail,
                             @NativeType("GLenum") int dpfail, @NativeType("GLenum") int dppass);

    void glStencilFuncSeparate(@NativeType("GLenum") int face, @NativeType("GLenum") int func,
                               @NativeType("GLint") int ref, @NativeType("GLuint") int mask);

    void glStencilMaskSeparate(@NativeType("GLenum") int face, @NativeType("GLuint") int mask);

    @NativeType("GLuint")
    int glCreateProgram();

    void glDeleteProgram(@NativeType("GLuint") int program);

    @NativeType("GLuint")
    int glCreateShader(@NativeType("GLenum") int type);

    void glDeleteShader(@NativeType("GLuint") int shader);

    void glAttachShader(@NativeType("GLuint") int program, @NativeType("GLuint") int shader);

    void glDetachShader(@NativeType("GLuint") int program, @NativeType("GLuint") int shader);

    void glShaderSource(@NativeType("GLuint") int shader, @NativeType("GLsizei") int count,
                        @NativeType("GLchar const * const *") long strings,
                        @NativeType("GLint const *") long length);

    void glCompileShader(@NativeType("GLuint") int shader);

    void glLinkProgram(@NativeType("GLuint") int program);

    void glUseProgram(@NativeType("GLuint") int program);

    @NativeType("void")
    int glGetShaderi(@NativeType("GLuint") int shader, @NativeType("GLenum") int pname);

    @NativeType("void")
    int glGetProgrami(@NativeType("GLuint") int program, @NativeType("GLenum") int pname);

    @NativeType("void")
    String glGetShaderInfoLog(@NativeType("GLuint") int shader);

    @NativeType("void")
    String glGetProgramInfoLog(@NativeType("GLuint") int program);

    @NativeType("GLint")
    int glGetUniformLocation(@NativeType("GLuint") int program, @NativeType("GLchar const *") CharSequence name);

    void glUniform1i(@NativeType("GLint") int location, @NativeType("GLint") int v0);

    void glEnableVertexAttribArray(@NativeType("GLuint") int index);

    void glVertexAttribPointer(@NativeType("GLuint") int index, @NativeType("GLint") int size,
                               @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized,
                               @NativeType("GLsizei") int stride, @NativeType("void const *") long pointer);

    void glVertexAttribIPointer(@NativeType("GLuint") int index, @NativeType("GLint") int size,
                                @NativeType("GLenum") int type, @NativeType("GLsizei") int stride,
                                @NativeType("void const *") long pointer);

    @NativeType("void")
    int glGenVertexArrays();

    void glDeleteVertexArrays(@NativeType("GLuint const *") int array);

    void glBindVertexArray(@NativeType("GLuint") int array);

    @NativeType("void")
    int glGenFramebuffers();

    void glDeleteFramebuffers(@NativeType("GLuint const *") int framebuffer);

    void glBindFramebuffer(@NativeType("GLenum") int target, @NativeType("GLuint") int framebuffer);

    @NativeType("GLenum")
    int glCheckFramebufferStatus(@NativeType("GLenum") int target);

    void glFramebufferTexture2D(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment,
                                @NativeType("GLenum") int textarget, @NativeType("GLuint") int texture,
                                @NativeType("GLint") int level);

    void glFramebufferRenderbuffer(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment,
                                   @NativeType("GLenum") int renderbuffertarget,
                                   @NativeType("GLuint") int renderbuffer);

    void glBlitFramebuffer(@NativeType("GLint") int srcX0, @NativeType("GLint") int srcY0,
                           @NativeType("GLint") int srcX1, @NativeType("GLint") int srcY1,
                           @NativeType("GLint") int dstX0, @NativeType("GLint") int dstY0,
                           @NativeType("GLint") int dstX1, @NativeType("GLint") int dstY1,
                           @NativeType("GLbitfield") int mask, @NativeType("GLenum") int filter);

    void glClearBufferiv(@NativeType("GLenum") int buffer, @NativeType("GLint") int drawbuffer,
                         @NativeType("GLint const *") IntBuffer value);

    void glClearBufferfv(@NativeType("GLenum") int buffer, @NativeType("GLint") int drawbuffer,
                         @NativeType("GLfloat const *") FloatBuffer value);

    void glClearBufferfi(@NativeType("GLenum") int buffer, @NativeType("GLint") int drawbuffer,
                         @NativeType("GLfloat") float depth, @NativeType("GLint") int stencil);

    void glBindBufferBase(@NativeType("GLenum") int target, @NativeType("GLuint") int index,
                          @NativeType("GLuint") int buffer);

    void glBindBufferRange(@NativeType("GLenum") int target, @NativeType("GLuint") int index,
                           @NativeType("GLuint") int buffer, @NativeType("GLintptr") long offset,
                           @NativeType("GLsizeiptr") long size);

    @NativeType("void")
    int glGenRenderbuffers();

    void glDeleteRenderbuffers(@NativeType("GLuint const *") int renderbuffer);

    void glBindRenderbuffer(@NativeType("GLenum") int target, @NativeType("GLuint") int renderbuffer);

    void glRenderbufferStorage(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat,
                               @NativeType("GLsizei") int width, @NativeType("GLsizei") int height);

    void glRenderbufferStorageMultisample(@NativeType("GLenum") int target, @NativeType("GLsizei") int samples,
                                          @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width,
                                          @NativeType("GLsizei") int height);

    @NativeType("void *")
    long glMapBufferRange(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset,
                          @NativeType("GLsizeiptr") long length, @NativeType("GLbitfield") int access);

    void glDrawArraysInstanced(@NativeType("GLenum") int mode, @NativeType("GLint") int first,
                               @NativeType("GLsizei") int count, @NativeType("GLsizei") int instancecount);

    void glDrawElementsInstanced(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count,
                                 @NativeType("GLenum") int type, @NativeType("void const *") long indices,
                                 @NativeType("GLsizei") int instancecount);

    void glCopyBufferSubData(@NativeType("GLenum") int readTarget, @NativeType("GLenum") int writeTarget,
                             @NativeType("GLintptr") long readOffset, @NativeType("GLintptr") long writeOffset,
                             @NativeType("GLsizeiptr") long size);

    @NativeType("GLuint")
    int glGetUniformBlockIndex(@NativeType("GLuint") int program,
                               @NativeType("GLchar const *") CharSequence uniformBlockName);

    void glUniformBlockBinding(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformBlockIndex,
                               @NativeType("GLuint") int uniformBlockBinding);

    @NativeType("GLsync")
    long glFenceSync(@NativeType("GLenum") int condition, @NativeType("GLbitfield") int flags);

    void glDeleteSync(@NativeType("GLsync") long sync);

    @NativeType("GLenum")
    int glClientWaitSync(@NativeType("GLsync") long sync, @NativeType("GLbitfield") int flags,
                         @NativeType("GLuint64") long timeout);

    @NativeType("void")
    int glGenSamplers();

    void glDeleteSamplers(@NativeType("GLuint const *") int sampler);

    void glBindSampler(@NativeType("GLuint") int unit, @NativeType("GLuint") int sampler);

    void glSamplerParameteri(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname,
                             @NativeType("GLint") int param);

    void glSamplerParameterf(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname,
                             @NativeType("GLfloat") float param);

    void glVertexAttribDivisor(@NativeType("GLuint") int index, @NativeType("GLuint") int divisor);
}
