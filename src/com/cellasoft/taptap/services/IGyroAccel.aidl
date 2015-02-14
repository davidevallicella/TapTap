package com.cellasoft.taptap.services;

interface IGyroAccel {
  oneway void statusMessage( in int state );
  oneway void diff( in double v, long timestamp );
}
