export class GpxTrack {
  public trk: Trk = new Trk();

  constructor(public id: number, public metadata: Metadata){}

  getName(v: GpxTrack): string {
    if (v.metadata.name !== undefined) { return v.metadata.name; }
    else if (v.trk.name !== undefined) { return v.trk.name; }
    else if (v.metadata.time !== undefined) { return v.metadata.time.toString(); }
    else { return '--'; }
  }

  addTrkseg(...trkpt: Trkpt[]): GpxTrack{
    this.trk.trkseg.push(new Trkseg(trkpt));
    return this;
  }
}

export class Metadata {
  constructor(public name: string, public time: Date){}
}

export class Trk {
  public name: string|undefined;
  public trkseg: Trkseg[] = [];
}

export class Trkseg {
  constructor(public trkpt: Trkpt[]){}
}

export class Trkpt {
  constructor(public lat: number, public lon: number, public ele: number, public time: Date){}

  getLatLon(): number[] {
    return [this.lat, this.lon];
  }
}


