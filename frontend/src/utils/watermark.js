/**
 * Watermark Utility
 * Creates a canvas-based watermark overlay
 */

const id = '1.2.3.4.5-watermark';

const setWatermark = (str) => {
    let id = '1.2.3.4.5-watermark';

    if (document.getElementById(id) !== null) {
        document.body.removeChild(document.getElementById(id));
    }

    const can = document.createElement('canvas');
    can.width = 300;
    can.height = 240;

    const cans = can.getContext('2d');
    cans.rotate(-20 * Math.PI / 180);
    cans.font = '15px Vedana';
    cans.fillStyle = 'rgba(200, 200, 200, 0.20)';
    cans.textAlign = 'left';
    cans.textBaseline = 'Middle';
    cans.fillText(str, can.width / 3, can.height / 2);

    const div = document.createElement('div');
    div.id = id;
    div.style.pointerEvents = 'none';
    div.style.top = '30px';
    div.style.left = '0px';
    div.style.position = 'fixed';
    div.style.zIndex = '10000000'; // High z-index to stay on top
    div.style.width = document.documentElement.clientWidth + 'px';
    div.style.height = document.documentElement.clientHeight + 'px';
    div.style.background = 'url(' + can.toDataURL('image/png') + ') left top repeat';
    document.body.appendChild(div);
    return id;
}

// Public wrapper
export const watermark = {
    // Set watermark
    set: (str) => {
        let id = setWatermark(str);
        if (document.getElementById(id) === null) {
            id = setWatermark(str);
        }
    },
    // Remove watermark
    remove: () => {
        if (document.getElementById(id) !== null) {
            document.body.removeChild(document.getElementById(id));
        }
    }
}
