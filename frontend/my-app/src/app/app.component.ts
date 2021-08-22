import { GpxTrack, Trkpt} from './gpxtrack';
import { GPXTRACKS } from './mock-tracks';
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { GpxStatistics } from './gpxStatistics';
import * as Highcharts from 'highcharts';
import { registerLocaleData } from '@angular/common';
import de from '@angular/common/locales/de';
import { thunderforest } from '../environments/thunderforestApiKey';
import { isDevMode } from '@angular/core';

declare let L: any;

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent /*implements OnInit*/ { // TODO load tracks to List
  static me: AppComponent;

  private readonly URL_GET_TRACKLIST = 'http://localhost:4201/api/getTracklist';
  private readonly URL_GET_POLYLINE = 'http://localhost:4201/api/getPolyLine?id=';
  private readonly URL_GET_ALL_TRACKS = 'http://localhost:4201/api/getAllTracksAsPolyLine';
  private readonly URL_GET_STATS = 'http://localhost:4201/api/getStatistics?id=';
  private readonly URL_GET_CHART_DATA = 'http://localhost:4201/api/getChartData?id=';

  title = 'GpxWerkzeug';
  tracks: TracklistTuple[] = [];
  shownTracks: TracklistTuple[] = [];
  selectedTrack: TracklistTuple|undefined;
  trackdata: GpxStatistics = new GpxStatistics();
  flatTrackLocations: Point[] = []; // used to reference from height profile to the right lat-lon
  trackFilterText = "";

  // Loading & spinner
  disableLoadButton = false;
  showTracksLoadingSpinner = false;
  tracklistFilter = "";

  // map
  map: any;
  mapMarkers: any = [];
  mapHighchartsMarker: any;

  // navigation
  navAllAriaCurrent = '';
  navTracksAriaCurrent = '';
  navAllActive = false;
  navTracksActive = true;

  // highcharts
  highcharts: typeof Highcharts = Highcharts;
  chartUpdateFlag = false;
  chartOptions: Highcharts.Options = {
    chart: { type: 'area', height: '300px' },
    title: { text: ''},
    subtitle: { text: '' },
    legend: { enabled: false },
    xAxis: {
      labels: {
        format: '{value} km'
      }
    },
    yAxis: [{
        title: { text: 'Elevation'},
        startOnTick: false,
        gridLineColor: '#E06969',
        labels: {
            rotation: -90,
            format: '{value}m',
        }
      } , { // Secondary axis
        title: { text: 'Speed'},
        gridLineDashStyle: 'Dot',
        gridLineColor: '#898CCC',
        labels: {
            rotation: 90,
            format: '{value}km/h',
        },
        opposite: true,
    }],
    plotOptions: {
        area: {
            pointStart: 0,
            marker: {
                enabled: false,
                symbol: 'circle',
                radius: 2,
                states: {
                    hover: {
                        enabled: true
                    }
                }
            }
        },
        series: {
          point: {
              events: {
                  mouseOver() {
                    console.log(this.index + ': ' + this.x + '-' + this.y);
                  }
              }
          }
        }
    },
    series: [{
      name: 'Elevation',
      yAxis: 0,
      color: '#E06969',
      type: 'area',
      data: [[1, 0], [2, 0]],
      tooltip: {
        headerFormat: '',
        pointFormat: 'Height: {point.y:,.0f}&thinsp;m / {point.x:,.2f}&thinsp;km'
      },
    } , {
      name: 'Velocity',
      yAxis: 1,
      color: '#898CCC',
      type: 'area',
      data: [[1, 0], [2, 0]],
      tooltip: {
        headerFormat: '',
        pointFormat: 'Speed: {point.y:,.1f}&thinsp;km/h / {point.x:,.2f}&thinsp;km'
      },
    }]
  };


  constructor(private http: HttpClient){  }

  setLoadingAnimation(loading: boolean): void {
    this.disableLoadButton = loading;
    this.showTracksLoadingSpinner = loading;
  }

  setTrackFilter(evt: any){
    this.trackFilterText = evt.target.value.toLowerCase();
    this.applyTrackFilter();
  }

  applyTrackFilter() {
    console.log("applyTrackFilter: " + this.trackFilterText);
    this.shownTracks = [];
    this.shownTracks = this.tracks
      .filter(t => t.name.toLowerCase().includes(this.trackFilterText));
  }

  onNavClick(item: string): void {
    console.log('click ' + item);
    // set nav bar class and aria correctly
    this.navAllAriaCurrent    = (item === 'all')    ? 'page' : '';
    this.navAllActive         = (item === 'all');
    this.navTracksAriaCurrent = (item === 'tracks') ? 'page' : '';
    this.navTracksActive      = (item === 'tracks');

    // Todo: these 2 should/could reside in one data structure?
    this.selectedTrack = undefined;
    this.flatTrackLocations = [];

    // reset map
    this.clearMap();

    if (item === 'all'){
      this.loadAllTracksToMap();
    }
  }

  loadAllTracksToMap(): void {
    this.setLoadingAnimation(true);
    this.http.get<number[][][]>(this.URL_GET_ALL_TRACKS).subscribe({
      next: (res) => this.showPolyLine(res),
      error: error => console.error(error),
      complete: () => {
        console.log('complete');
        this.setLoadingAnimation(false);
      }
    });
  }

  onClickReloadTracks(): void {
    console.log('(re)load tracks');
    this.setLoadingAnimation(true);
    this.tracks = [];
    this.http.get<TracklistTuple[]>(this.URL_GET_TRACKLIST).subscribe({
      next: (res) => res.forEach(t => {
        this.tracks.push(t);
        this.applyTrackFilter();
      }),
      error: error => console.error(error),
      complete: () => this.setLoadingAnimation(false)
    });
  }

  onSelect(track: TracklistTuple): void {
    this.selectedTrack = track;
    this.showTrack(track.id);
    this.showTrackStatistics(track.id);
    this.showProfileChart(track.id);
    this.registerHighchartsMouseListener();
  }

  showTrack(id: number): void{
    console.log('get polyLine for ' + id);
    this.http.get<number[][][]>(this.URL_GET_POLYLINE + id).subscribe({
      next: (res) => {
        this.flatTrackLocations = this.flattenPolyLine(res);
        this.showPolyLine(res);
      },
      error: error => console.error(error),
      complete: () => console.log('complete')
    });
  }

  showProfileChart(id: number): void {
    console.log('get chart-data for ' + id);
    this.http.get<ChartData>(this.URL_GET_CHART_DATA + id).subscribe({
      next: (res) => {
        if (this.chartOptions.series !== undefined) {
          const dist = res.distance.map(v => v / 1000.0); // km
          const elev = res.elevation.map((v, i) => [dist[i], v]);
          const velo = res.velocity.map((v, i) => [dist[i], v]);
          // let the compiler know the type
          if (this.chartOptions.series[0].type === 'area' && this.chartOptions.series[1].type === 'area') {
            this.chartOptions.series[0].data = elev; // elevation
            this.chartOptions.series[1].data = velo; // velocity
          }

          // Set the elevation min
          if (this.chartOptions.yAxis !== undefined){
            const opt: any = this.chartOptions; // get rid of an typescript error in the next line
            opt.yAxis[0].min = Math.min.apply(Math, res.elevation);
          }
          this.chartUpdateFlag = true;
        }
      },
      error: error => console.error(error),
      complete: () => console.log('complete')
    });
  }

  registerHighchartsMouseListener(): void {
    // OMFG this.chartOptions.plotOptions?.series?.point?.events?.mouseOver;
    if (this.chartOptions.plotOptions !== undefined &&
      this.chartOptions.plotOptions.series !== undefined &&
      this.chartOptions.plotOptions.series.point !== undefined &&
      this.chartOptions.plotOptions.series.point.events !== undefined ){
        if (this.chartOptions.plotOptions.series.point.events.mouseOver !== undefined){
          this.chartOptions.plotOptions.series.point.events.mouseOver = function(){
            AppComponent.me.setMarkerToIndex(this.index);
          };
        }
        if (this.chartOptions.plotOptions.series.point.events.mouseOut !== undefined){
          this.chartOptions.plotOptions.series.point.events.mouseOut = function(){
            AppComponent.me.removeMapMarker();
          };
        }
        }
  }

  // removes the map marker indicating the mouse-over location in highcharts
  removeMapMarker(): void {
    if (this.mapHighchartsMarker !== undefined) {
      this.map.removeLayer(this.mapHighchartsMarker);
    }
  }

  // set the map marker indicating the mouse-over location in highcharts
  setMarkerToIndex(i: number): void {
    this.removeMapMarker();
    if (this.flatTrackLocations === undefined) {
      console.error('no tracklocations to map to');
      return;
    }

    // https://leafletjs.com/reference-1.6.0.html#divicon-option
    const myicon = L.divIcon({
      className: 'map-custom-icon',
      html: '<div></div>',
      iconSize: [15, 15]
    });
    const p = this.flatTrackLocations[i];
    this.mapHighchartsMarker = L.marker([p.lat, p.lon], { icon: myicon, draggable: false });
    this.mapHighchartsMarker.addTo(this.map);
    this.map.setView([p.lat, p.lon], this.map.getZoom(), {
      animate: true,
      pan: { duration: 1 }
    });
  }

  showTrackStatistics(id: number): void {
    this.http.get<GpxStatistics>(this.URL_GET_STATS + id).subscribe({
      next: (res) => this.trackdata = res,
      error: error => console.error(error),
      complete: () => console.log('complete')
    });
  }

  clearMap(): void {
    for (const e of this.mapMarkers) { e.remove(); }
    this.mapMarkers = [];
  }

  flattenPolyLine(track: number[][][]): Point[] {
    const out: Point[] = [];
    track.forEach(seg =>
      seg.forEach(p => out.push(new Point(p[0], p[1]))
      )
    );
    return out;
  }

  showPolyLine(points: number[][][]): void {
    this.clearMap();
    // https://leafletjs.com/reference-1.7.1.html#polyline
    // https://leafletjs.com/reference-1.6.0.html#polyline-option
    const polyline = L.polyline(points, {
      color: '#5555ff',
      smoothFactor: 1,
      weight: 3
    });
    this.mapMarkers.push(polyline);
    this.map.fitBounds(polyline.getBounds(), {
      animate: true,
      pan: { duration: 1 }
    });
    polyline.addTo(this.map);
  }

  initMap(): void {
    this.map = L.map('mapid').setView([47.7348898, 11.5742609], 13);
    // https://opentopomap.org/about#verwendung
    // L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', {
    //     attribution: 'Kartendaten: © <a href="https://openstreetmap.org/copyright">OpenStreetMap</a>-Mitwirkende, SRTM | Kartendarstellung: © <a href="http://opentopomap.org">OpenTopoMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)'
    // }).addTo(this.map);
    // http://leaflet-extras.github.io/leaflet-providers/preview/
    L.tileLayer('https://{s}.tile.thunderforest.com/landscape/{z}/{x}/{y}.png?apikey={apikey}', {
      attribution: '&copy; <a href="http://www.thunderforest.com/">Thunderforest</a>, &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      apikey: thunderforest.apikey,
      maxZoom: 22,
      trackResize: true
    }).addTo(this.map);
  }

  ngOnInit(): void {
    if (isDevMode()) {
      console.log('👋 Development!');
    } else {
      console.log('💪 Production!');
    }
    AppComponent.me = this;
    registerLocaleData( de );
    this.initMap();
    this.onClickReloadTracks();
  }
}

export class TracklistTuple{
  constructor(public id: number, public name: string, public  timestamp: number){}
}

class Point{
  constructor(public lat: number, public lon: number){}
}

class ChartData{
  constructor(public elevation: number[], public velocity: number[], public distance: number[]){}
}
