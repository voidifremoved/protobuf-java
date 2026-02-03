/**
 * Implementation of the Protocol Buffer compiler.
 *
 * <p>This package contains code for parsing .proto files and generating code based on them. There
 * are two reasons you might be interested in this package:
 *
 * <ul>
 *   <li>You want to parse .proto files at runtime. In this case, you should look at {@code
 *       Importer}. Since this functionality is widely useful, it is included in the libprotobuf
 *       base library; you do not have to link against libprotoc.
 *   <li>You want to write a custom protocol compiler which generates different kinds of code, e.g.
 *       code in a different language which is not supported by the official compiler. For this
 *       purpose, {@code CommandLineInterface} provides you with a complete compiler front-end, so
 *       all you need to do is write a custom implementation of {@code CodeGenerator} and a trivial
 *       main() function. You can even make your compiler support the official languages in
 *       addition to your own. Since this functionality is only useful to those writing custom
 *       compilers, it is in a separate library called "libprotoc" which you will have to link
 *       against.
 * </ul>
 */
package com.rubberjam.protobuf.compiler;
