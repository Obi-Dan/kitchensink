# Coding Standards and Style Guide

This document outlines the coding standards and style requirements for the Kitchensink application. These standards are enforced by the checkstyle configuration in the project.

## General Requirements

1. **No trailing whitespace**: Lines must not end with whitespace characters.
2. **No tabs**: Use spaces for indentation, not tab characters.

## Import Requirements

1. **No wildcard imports**: Avoid using wildcard imports (e.g., `import java.util.*`). The only exception is for static member imports.
2. **No redundant imports**: Do not include multiple imports for the same class.
3. **No unused imports**: Do not include imports for classes that aren't used.
4. **No illegal imports**: Certain packages are prohibited (e.g., `junit.framework`).

## Code Style Requirements

1. **Modifier order**: Modifiers must follow the Java Language Specification order: public, protected, private, abstract, static, final, transient, volatile, synchronized, native, strictfp.
2. **No redundant modifiers**: Avoid using modifiers when they are redundant.
3. **Left curly brace placement**: For class, constructor, interface, method definitions, and switch statements, the left curly brace must be at the end of the line.
4. **Upper case for long literals**: Long literals must use an uppercase 'L' (e.g., `100L` not `100l`).
5. **Array brackets**: Use Java style for array declarations (e.g., `String[] args` not `String args[]`).

## Coding Practices

1. **No empty statements**: Avoid empty statements (e.g., unnecessary semicolons).
2. **No illegal instantiations**: Avoid direct instantiation of types that should be created through factory methods.
3. **Package annotations**: Annotations on package declarations are valid.
4. **No suppressing all warnings**: The annotation `@SuppressWarnings("all")` is not allowed.

## Eclipse/IDE Setup

It's recommended to configure your IDE to:

1. Automatically remove trailing whitespace when saving files
2. Use 4 spaces for indentation
3. Enable automatic organizing of imports
4. Apply the project's checkstyle configuration

## Maven Commands

To check your code for style issues:

```
mvn checkstyle:check
```

To automatically fix some common issues (note: doesn't fix all problems):

```
sed -i '' 's/ *$//' path/to/file.java  # Removes trailing spaces
```

## Adding New Code

When adding new code to the project:

1. Ensure it follows these style guidelines
2. Run checkstyle before committing
3. Fix any issues that are identified
4. Make sure the build passes with `mvn clean install` 