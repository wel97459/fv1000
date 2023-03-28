#include <SDL2/SDL.h>
#include <SDL2/SDL_ttf.h>
#include <memory>
#include <vector>
#include <pthread.h>
#include <thread>
#include "sim.h"
#include "crt.h"

#include <verilated_fst_c.h>
#include "Vfv1000.h"

#define COLOR_LEVEL (WHITE_LEVEL - 20)
int cc[4] = {BLANK_LEVEL, BURST_LEVEL, BLANK_LEVEL, -BURST_LEVEL};
float cc1[4] = {0, 1, 0, -1};

void (*sim_draw)();
Uint32 *screenPixels;
SDL_Texture *screen;
int *sim_video;
struct CRT *sim_crt;
static uint64_t vidTime = 0;
static int PhaseOffset = 1;
static struct IIRLP iirY, iirI, iirQ;

VerilatedFstC* m_trace;
Vfv1000 fv1000;

Uint64 main_time=0;
Uint64 main_trace=0;
Uint8 trace=0;

Uint8 rom[0x4000];
Uint8 ram[0x8000];

Uint8 VSync_Edge=0;
Uint16 FrameCount = 0;
Uint16 FrameCurent = 0;

Uint64 ticksLast = 0 ;
char tmpstr[64];

int loadFile(const char *filename, Uint8 *pointer, const Uint32 len)
{
    FILE *fp = fopen(filename, "r");
    if ( fp == 0 )
    {
        printf( "Could not open file\n" );
        return -1;
    }

    fseek(fp, 0L, SEEK_END);
    Uint32 fsize = ftell(fp);
    fseek(fp, 0L, SEEK_SET);

    if(fsize > len){
        printf("File is to big!\n");
        fclose(fp);
        return -2;
    }

    size_t s = fread(pointer, 1, fsize, fp);
    fclose(fp);

    return 0;
}

int saveFile(const char *filename, Uint8 *pointer, const Uint32 len)
{
    FILE *fp = fopen(filename, "w+");
    if ( fp == 0 )
    {
        printf( "Could not open file\n" );
        return -1;
    }
    size_t s = fwrite(pointer, 1, len, fp);
    fclose(fp);

    return 0;
}

void sim_init(int *v, SDL_Texture *td, void (*d)(), struct CRT *c){
    //screenPixels = p;
    sim_draw = d;
    screen = td;
    sim_video = v;
    sim_crt = c;

    SDL_UpdateTexture(screen, NULL, screenPixels, 240 * sizeof(Uint32));
    sim_draw();

    printf("Started. %lu\n", LINE_ns);

    loadFile("../data/testing/test.bin", rom, 0x4000);

	#ifdef TRACE
		Verilated::traceEverOn(true);
		m_trace = new VerilatedFstC;
		fv1000.trace(m_trace, 99);
		m_trace->open ("simx.fst");
	#endif

    init_iir(&iirY, L_FREQ, Y_FREQ);
    init_iir(&iirI, L_FREQ, I_FREQ);
    init_iir(&iirQ, L_FREQ, Q_FREQ);
}

void sim_keyevent(int key){
    // if (key == SDLK_9 && colors.index > 0) {
    //     colors.index -= 1;
    //     printf("Index:%i\n", colors.index);
    // }
    // if (key == SDLK_0 && colors.index < 8) {
    //     colors.index += 1;
    //     printf("Index:%i\n", colors.index);
    // }
    // if (key == SDLK_o) {
    //     colors.Amplitude[colors.index] -= 1000;
    //     printf("Amplitude[%u]:%i\n",colors.index, colors.Amplitude[colors.index]);
    // }
    // if (key == SDLK_p) {
    //     colors.Amplitude[colors.index] += 1000;
    //     printf("Amplitude[%u]:%i\n",colors.index, colors.Amplitude[colors.index]);
    // }
    // if (key == SDLK_k) {
    //     colors.Phase[colors.index]-= 50;
    //     printf("Phase[%u]:%i\n",colors.index, colors.Phase[colors.index]);
    // }
    // if (key == SDLK_l) {
    //     colors.Phase[colors.index]+= 50;
    //     printf("Phase[%u]:%i\n",colors.index, colors.Phase[colors.index]);
    // }
    // if (key == SDLK_n) {
    //     colors.PhaseAmp[colors.index]-= 50;
    //     printf("PhaseAmp[%u]:%i\n",colors.index, colors.PhaseAmp[colors.index]);
    // }
    // if (key == SDLK_m) {
    //     colors.PhaseAmp[colors.index]+= 50;
    //     printf("PhaseAmp[%u]:%i\n",colors.index, colors.PhaseAmp[colors.index]);
    // }
}

Uint32 colorsRGB[]={
    0x00000000,
    0x0000FF00,
    0x000000FF,
    0x0000FFFF,
    0x00FF0000,
    0x00FFFF00,
    0x00FF00FF,
    0x00FFFFFF,
};

void doNTSC(int CompSync, int Video, int Burst, int Color)
{	
    int ire = -40, fi, fq, fy;
    int pA;
    int rA, gA, bA;
    int rB = 127, gB = 127, bB = 127;
	if(CompSync) ire=BLANK_LEVEL;
	if(Video) ire=WHITE_LEVEL;
    uint32_t i;
    for (i = ns2pos(vidTime); i < ns2pos(vidTime+DOT_ns); i++)
    {
        if(Burst) ire = cc[i&3];
        
        if(Color > 0) {
            ire = BLACK_LEVEL ;

            pA = colorsRGB[Color];
            bA = (pA >> 16) & 0xff;
            gA = (pA >>  8) & 0xff;
            rA = (pA >>  0) & 0xff;

            fy = (19595 * rA + 38470 * gA +  7471 * bA) >> 14;
            fi = (39059 * rA - 18022 * gA - 21103 * bA) >> 14;
            fq = (13894 * rA - 34275 * gA + 20382 * bA) >> 14;

            fy = fy;
            fi = fi * cc1[(i + 0) & 3];
            fq = fq * cc1[(i + 3) & 3];

            ire += (fy + fi + fq) * (WHITE_LEVEL * 100 / 100) >> 10;;
            if (ire < 0)   ire = 0;
            if (ire > 110) ire = 110;
        }

        sim_crt->analog[i] = ire;
        fv1000.io_Video = ire;
        fv1000.eval();
        main_trace++;
        m_trace->dump (main_trace);
    }

    vidTime+=DOT_ns;
	return;
}

void clock(uint c)
{
    main_time++;
    fv1000.clk = c;
    fv1000.eval();

    #ifdef TRACE
        if(trace){
            main_trace++;
            m_trace->dump (main_trace);
        }
    #endif
}

void sim_run(){
    fv1000.reset = !(main_time>10);
    doNTSC(fv1000.io_CompSync_, 0, fv1000.io_Burst, fv1000.io_Pixel ? 3 : 0);
    if(!fv1000.io_VSync_ && VSync_Edge){
        sim_draw();
        sprintf(tmpstr,"Frames/Frame%04i.png",FrameCount++);
        //Uint64 ticks = SDL_GetTicks64();
        printf("Frame: %i\n", FrameCount);
        //ticksLast = ticks;
        //screenshot(tmpstr);
        vidTime = 0;
        memset(sim_crt->analog, 0, CRT_INPUT_SIZE);
    }
    VSync_Edge = fv1000.io_VSync_;
    clock(1);
    clock(0);
}

void sim_end()
{
    printf("Ended.\n");
    fv1000.final();

    #ifdef TRACE
        m_trace->close();
    #endif
}