import { GpxTrack, Trkpt, Metadata} from './gpxtrack';

const t1 = new GpxTrack(0, new Metadata('somewhere', new Date('2020-11-22T11:01:32Z')))
  .addTrkseg(
    new Trkpt(47.7343957499, 11.5727781225, 538.48, new Date('2020-11-22T11:01:32Z')),
    new Trkpt(47.7342802472, 11.5730849840, 539.44, new Date('2020-11-22T11:02:10Z')),
    new Trkpt(47.7337522712, 11.5763392579, 555.30, new Date('2020-11-22T11:03:34Z'))
  );

const t2 = new GpxTrack(1, new Metadata('s.w else', new Date('2020-11-24T13:01:32Z')))
  .addTrkseg(
    new Trkpt(47.7353957499, 11.5737781225, 538.48, new Date('2020-11-24T11:01:32Z')),
    new Trkpt(47.7352802472, 11.5740849840, 539.44, new Date('2020-11-24T11:02:10Z')),
    new Trkpt(47.7357522712, 11.5773392579, 555.30, new Date('2020-11-24T11:03:34Z'))
  )
  .addTrkseg(
    new Trkpt(47.7363957499, 11.5737781225, 538.48, new Date('2020-11-24T12:01:32Z')),
    new Trkpt(47.7362802472, 11.5740849840, 539.44, new Date('2020-11-24T12:02:10Z')),
    new Trkpt(47.7367522712, 11.5773392579, 555.30, new Date('2020-11-24T12:03:34Z'))
  );

export const GPXTRACKS: GpxTrack[] = [t1, t2];
