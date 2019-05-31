 %module sdd
 %{
 /* Includes the header in the wrapper code */
 #include "include/sddapi.h"
 %}
 
 /* Parse the header file to generate wrappers */
 %include "include/sddapi.h"
 %include "carrays.i"
 %array_functions(long, longArray)
 %array_functions(int, intArray)
 %array_functions(SddNode*, sddNodeArray)
