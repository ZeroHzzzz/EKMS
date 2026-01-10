/**
 * Watermark Utility
 * Creates a canvas-based watermark overlay
 */

const id = '1.2.3.4.5-watermark';

const setWatermark = (str, container) => {
    let id = '1.2.3.4.5-watermark';

    if (container) {
        if (container.querySelector('#' + id) !== null) {
            container.removeChild(container.querySelector('#' + id));
        }
    } else {
        if (document.getElementById(id) !== null) {
            document.body.removeChild(document.getElementById(id));
        }
    }

    const can = document.createElement('canvas');
    can.width = 300;
    can.height = 240;

    const cans = can.getContext('2d');
    cans.rotate(-20 * Math.PI / 180);
    cans.font = '15px Vedana';
    cans.fillStyle = 'rgba(100, 100, 100, 0.30)'; // Darker and more opaque for visibility against white
    cans.textAlign = 'left';
    cans.textBaseline = 'Middle';
    cans.fillText(str, can.width / 3, can.height / 2);

    const div = document.createElement('div');
    div.id = id;
    div.style.pointerEvents = 'none';
    div.style.top = '0px';
    div.style.left = '0px';
    div.style.background = 'url(' + can.toDataURL('image/png') + ') left top repeat';

    if (container) {
        div.style.position = 'absolute';
        div.style.zIndex = '9999'; // Ensure it's on top of iframes/viewers
        div.style.width = '100%';
        div.style.height = '100%';
        // Ensure container is relative so absolute positioning works
        if (getComputedStyle(container).position === 'static') {
            container.style.position = 'relative';
        }
        container.appendChild(div);
    } else {
        div.style.position = 'fixed';
        div.style.zIndex = '10000000';
        div.style.width = document.documentElement.clientWidth + 'px';
        div.style.height = document.documentElement.clientHeight + 'px';
        document.body.appendChild(div);
    }

    return id;
}

// Public wrapper
export const watermark = {
    // Set watermark
    set: (str, container) => {
        let id = setWatermark(str, container);
        if (!container && document.getElementById(id) === null) {
            id = setWatermark(str, container);
        }
    },
    // Remove watermark
    remove: () => {
        if (document.getElementById(id) !== null) {
            document.body.removeChild(document.getElementById(id));
        }
    }
}
