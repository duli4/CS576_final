the sample_audio.pcm files are only for testing 

In this refrence code, it can only play pcm files and you need to modify it using it as a starting code so that wav files can be run correctly. We suggest you use the Visual Studio 2019. 

It is highly possible that you get some error(This may help you to run it successfully):

In winnt.h file
Modify Ln 290: typedef void * POINTER_64 PVOID64;

To:
typedef void * POINTER_64;
typedef void * PVOID64;